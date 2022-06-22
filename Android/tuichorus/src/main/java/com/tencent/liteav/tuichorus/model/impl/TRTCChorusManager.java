package com.tencent.liteav.tuichorus.model.impl;

import static com.tencent.live2.V2TXLiveCode.V2TXLIVE_ERROR_DISCONNECTED;
import static com.tencent.live2.V2TXLiveCode.V2TXLIVE_OK;
import static com.tencent.live2.V2TXLiveDef.V2TXLiveAudioQuality.V2TXLiveAudioQualityDefault;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import com.tencent.liteav.audio.TXAudioEffectManager;
import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.live2.V2TXLiveDef;
import com.tencent.live2.V2TXLivePlayer;
import com.tencent.live2.V2TXLivePlayerObserver;
import com.tencent.live2.V2TXLivePusher;
import com.tencent.live2.V2TXLivePusherObserver;
import com.tencent.live2.impl.V2TXLivePlayerImpl;
import com.tencent.rtmp.TXLiveBase;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.util.Timer;
import java.util.TimerTask;

public class TRTCChorusManager implements TXAudioEffectManager.TXMusicPlayObserver {

    private static final String TAG = "TRTCChorusManager";

    private static final String KEY_CMD                 = "cmd";
    private static final String KEY_MUSIC_ID            = "music_id";
    private static final String KEY_START_PLAY_MUSIC_TS = "startPlayMusicTS";
    private static final String KEY_REQUEST_STOP_TS     = "requestStopTS";
    private static final String KEY_MUSIC_CURRENT_TS    = "musicCurrentTS";
    private static final String KEY_MUSIC_CURRENT_ID    = "musicCurrentId";
    private static final String KEY_MUSIC_DURATION      = "music_duration";
    private static final String MSG_START_CHORUS        = "startChorus";
    private static final String MSG_STOP_CHORUS         = "stopChorus";
    private static final int    MUSIC_START_DELAY       = 3000;
    private static final int    MUSIC_PRELOAD_DELAY     = 400;
    private static final int    MESSAGE_SEND_INTERVAL   = 1000;
    private static final float  CACHE_TIME_SMOOTH       = 5.0f;
    private static final int    MUSIC_DEFAULT_VOLUMN    = 80;

    private static final String V2TXLIVEPUSHER_PACKAGE_NAME = "com.tencent.live2.impl.V2TXLivePusherImpl";

    private final Context            mContext;
    private final TRTCCloud          mTRTCCloud;
    private       Timer              mTimer;
    private final HandlerThread      mWorkThread;
    private final Handler            mWorkHandler;
    private       TRTCChorusListener mListener;

    /**
     * 合唱音乐相关
     */
    private          String  mMusicPath;
    private          int     mMusicID;
    private volatile long    mMusicDuration;
    private volatile boolean mIsChorusOn;
    private          long    mRevStartPlayMusicTs;
    private volatile long    mStartPlayMusicTs;
    private          long    mRequestStopPlayMusicTs;

    private ChorusStartReason mChorusStartReason = ChorusStartReason.LocalStart;//记录开始合唱的原因，可以确定播放者身份:房主/副唱

    /**
     * 合唱 cdn 相关
     */
    private              V2TXLivePusher mPusher;
    private              V2TXLivePlayer mPlayer;
    private static final int            SEI_PAYLOAD_TYPE = 242;
    private static final int            SEI_LRC_OFFSET   = -100; //听众端歌词校准补偿值

    public TRTCChorusManager(@NonNull Context context, @NonNull TRTCCloud trtcCloud) {
        mContext = context;
        mTRTCCloud = trtcCloud;
        mWorkThread = new HandlerThread("TRTCChorusManagerWorkThread");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper());
        initPusher();
    }

    /**
     * 设置合唱回调
     *
     * @param listener 合唱回调
     */
    public void setListener(TRTCChorusListener listener) {
        mListener = listener;
    }

    /**
     * 开始合唱
     *
     * @return true：合唱启动成功；false：合唱启动失败
     */
    public boolean startChorus(int musicId, String musicPath) {
        TXCLog.i(TAG, "startChorus");
        this.mMusicID = musicId;
        this.mMusicPath = musicPath;
        mMusicDuration = mTRTCCloud.getAudioEffectManager().getMusicDurationInMS(mMusicPath);
        boolean result;
        if (mChorusStartReason == ChorusStartReason.LocalStart) {
            result = startPlayMusic(mChorusStartReason, MUSIC_START_DELAY);
        } else {
            result = startPlayMusic(mChorusStartReason, (int) (mRevStartPlayMusicTs - getNtpTime()));
        }
        return result;
    }

    /**
     * 停止合唱
     */
    public void stopChorus() {
        TXCLog.i(TAG, "stopChorus");
        clearStatus();
        stopPlayMusic(ChorusStopReason.LocalStop);
    }

    /**
     * 清除状态
     */
    public void clearStatus() {
        mChorusStartReason = ChorusStartReason.LocalStart;
    }

    /**
     * 当前是否正在合唱
     *
     * @return true：当前正在合唱中；false：当前不在合唱
     */
    public boolean isChorusOn() {
        return mIsChorusOn;
    }

    /**
     * TRTC 自定义消息回调，用于接收房间内其他用户发送的自定义消息，用于解析处理合唱相关消息
     *
     * @param userId  用户标识
     * @param cmdID   命令 ID
     * @param seq     消息序号
     * @param message 消息数据
     */
    public void onRecvCustomCmdMsg(String userId, int cmdID, int seq, byte[] message) {
        TXCLog.i(TAG, "onRecvCustomCmdMsg userId : " + userId + " , message : " + message);
        if (!isNtpReady() || message == null || message.length <= 0) {
            return;
        }
        try {
            JSONObject json = new JSONObject(new String(message, "UTF-8"));
            if (!json.has(KEY_CMD)) {
                return;
            }
            switch (json.getString(KEY_CMD)) {
                case MSG_START_CHORUS:
                    mRevStartPlayMusicTs = json.getLong(KEY_START_PLAY_MUSIC_TS);
                    if (mRevStartPlayMusicTs < mRequestStopPlayMusicTs) {
                        // 当前收到的命令是在请求停止合唱之前发出的，需要忽略掉，否则会导致请求停止后又开启了合唱
                        return;
                    }
                    if (!mIsChorusOn && mListener != null) {
                        mChorusStartReason = ChorusStartReason.RemoteStart;
                        mListener.onReceiveAnchorSendChorusMsg(json.optString(KEY_MUSIC_ID), mRevStartPlayMusicTs - getNtpTime());
                    }
                    TXCLog.i(TAG, "receive start chorus message. startTs:" + mRevStartPlayMusicTs);
                    break;
                case MSG_STOP_CHORUS:
                    mRequestStopPlayMusicTs = json.getLong(KEY_REQUEST_STOP_TS);
                    TXCLog.i(TAG, "receive stop chorus message. stopTs:" + mRequestStopPlayMusicTs);
                    stopPlayMusic(ChorusStopReason.RemoteStop);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            TXCLog.e(TAG, "parse custom message failed. " + e);
        }
    }

    /**
     * 开始合唱 CDN 推流
     *
     * @param url 推流地址
     * @return true：推流成功；false：推流失败
     */
    public boolean startCdnPush(String url) {
        TXCLog.i(TAG, "startCdnPush url:" + url);
        initPusher();
        if (!url.endsWith("&enableblackstream=1")) {
            url = url + "&enableblackstream=1";
        }
        if (mPusher == null) {
            TXCLog.i(TAG, "startCdnPush mPush is null");
            return false;
        }
        if (mPusher.isPushing() == 1) {
            return false;
        }
        mPusher.startVirtualCamera(null);
        return mPusher.startPush(url) == V2TXLIVE_OK;
    }

    /**
     * 停止合唱 CDN 推流
     */
    public void stopCdnPush() {
        TXCLog.i(TAG, "stopCdnPush");
        if (mPusher == null) {
            return;
        }
        if (mPusher.isPushing() == 1) {
            mPusher.stopVirtualCamera();
            mPusher.stopPush();
        }
    }

    /**
     * 是否正在 CDN 推流中
     *
     * @return true：正在推流；false：不在推流
     */
    public boolean isCdnPushing() {
        initPusher();
        return mPusher.isPushing() == 1;
    }

    /**
     * 开始合唱 CDN 播放
     *
     * @param url  拉流地址
     * @param view 承载视频的 view
     * @return true：拉流成功；false：拉流失败
     */
    public boolean startCdnPlay(String url, TXCloudVideoView view) {
        TXCLog.i(TAG, "startCdnPlay url:" + url);
        initPlayer();
        if (mPlayer.isPlaying() == 1) {
            return false;
        }
        mPlayer.setRenderView(view);
        mPlayer.setCacheParams(CACHE_TIME_SMOOTH, CACHE_TIME_SMOOTH);
        return mPlayer.startPlay(url) == V2TXLIVE_OK;
    }

    /**
     * 停止合唱 CDN 播放
     */
    public void stopCdnPlay() {
        TXCLog.i(TAG, "stopCdnPlay");
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying() == 1) {
            mPlayer.stopPlay();
            mListener.onMusicCompletePlaying(mMusicID);
        }
    }

    /**
     * 是否正在 CDN 播放中
     *
     * @return true：正在播放；false：不在播放
     */
    public boolean isCdnPlaying() {
        initPlayer();
        return mPlayer.isPlaying() == 1;
    }

    /////////////////////////////////////////////////////////////////////////////////
    //
    //                    私有方法
    //
    /////////////////////////////////////////////////////////////////////////////////

    private void preloadMusic(int startTimeMS) {
        TXCLog.i(TAG, "preloadMusic currentNtp:" + getNtpTime());
        String body = "";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("api", "preloadMusic");
            JSONObject paramJsonObject = new JSONObject();
            paramJsonObject.put("musicId", mMusicID);
            paramJsonObject.put("path", mMusicPath);
            paramJsonObject.put("startTimeMS", startTimeMS);
            jsonObject.put("params", paramJsonObject);
            body = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mTRTCCloud.callExperimentalAPI(body);
    }

    private boolean isNtpReady() {
        return TXLiveBase.getNetworkTimestamp() > 0;
    }

    private long getNtpTime() {
        return TXLiveBase.getNetworkTimestamp();
    }

    private boolean startPlayMusic(ChorusStartReason reason, int delayMs) {
        if (!isNtpReady() || mMusicDuration <= 0) {
            TXCLog.e(TAG, "startPlayMusic failed. isNtpReady:" + isNtpReady() + " isMusicFileReady:" + (mMusicDuration > 0));
            return false;
        }
        if (delayMs <= -mMusicDuration) {
            //若 delayMs 为负数，代表约定的合唱开始时间在当前时刻之前
            //进一步，若 delayMs 为负，并且绝对值大于 BGM 时长，证明此时合唱已经结束了，应当忽略此次消息
            return false;
        }
        if (mIsChorusOn) {
            return false;
        }
        mIsChorusOn = true;
        TXCLog.i(TAG, "startPlayMusic delayMs:" + delayMs + " mMusicDuration:" + mMusicDuration);

        startTimer(reason, reason == ChorusStartReason.LocalStart ? (getNtpTime() + MUSIC_START_DELAY) : mRevStartPlayMusicTs);
        final TXAudioEffectManager.AudioMusicParam audioMusicParam = new TXAudioEffectManager.AudioMusicParam(mMusicID, mMusicPath);
        audioMusicParam.publish = false;
        audioMusicParam.loopCount = 0;
        mTRTCCloud.getAudioEffectManager().setMusicObserver(mMusicID, this);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!mIsChorusOn) {
                    // 若达到预期播放时间时，合唱已被停止，则跳过此次播放
                    return;
                }
                TXCLog.i(TAG, "calling startPlayMusic currentNtp:" + getNtpTime());
                mTRTCCloud.getAudioEffectManager().startPlayMusic(audioMusicParam);
                mTRTCCloud.getAudioEffectManager().setMusicPlayoutVolume(audioMusicParam.id, MUSIC_DEFAULT_VOLUMN);
                mTRTCCloud.getAudioEffectManager().setMusicPublishVolume(audioMusicParam.id, 0);
            }
        };

        if (delayMs > 0) {
            preloadMusic(0);
            mWorkHandler.postDelayed(runnable, delayMs);
        } else {
            preloadMusic(Math.abs(delayMs) + MUSIC_PRELOAD_DELAY);
            mWorkHandler.postDelayed(runnable, MUSIC_PRELOAD_DELAY);
        }
        if (mListener != null) {
            mListener.onChorusStart(reason);
        }
        return true;
    }

    private void startTimer(final ChorusStartReason reason, final long startTs) {
        TXCLog.i(TAG, "startTimer startTs:" + startTs);
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //若本地开始播放，则发送合唱信令
                    if (reason == ChorusStartReason.LocalStart) {
                        sendStartMusicMsg(startTs);
                    }
                    checkMusicProgress();
                }
            }, 0, MESSAGE_SEND_INTERVAL);
            mStartPlayMusicTs = startTs;
        }
    }

    private void sendStartMusicMsg(long startTs) {
        String body = "";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(KEY_CMD, MSG_START_CHORUS);
            jsonObject.put(KEY_START_PLAY_MUSIC_TS, startTs);
            jsonObject.put(KEY_MUSIC_ID, String.valueOf(mMusicID));
            jsonObject.put(KEY_MUSIC_DURATION, String.valueOf(mMusicDuration));
            body = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mTRTCCloud.sendCustomCmdMsg(0, body.getBytes(), false, false);
    }

    private void stopPlayMusic(ChorusStopReason reason) {
        if (!mIsChorusOn) {
            return;
        }
        mWorkHandler.removeCallbacksAndMessages(null);
        mIsChorusOn = false;
        TXCLog.i(TAG, "stopPlayMusic reason:" + reason);
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        mTRTCCloud.getAudioEffectManager().setMusicObserver(mMusicID, null);
        mTRTCCloud.getAudioEffectManager().stopPlayMusic(mMusicID);
        if (reason == ChorusStopReason.LocalStop && mChorusStartReason == ChorusStartReason.LocalStart) {
            sendStopBgmMsg();
        }
        //停止播放时,清空合唱停止时间信息
        if (reason == ChorusStopReason.LocalStop) {
            mRequestStopPlayMusicTs = 0;
        }
        if (mListener != null) {
            mListener.onChorusStop(reason);
        }
        mListener.onMusicCompletePlaying(mMusicID);
    }

    private void sendStopBgmMsg() {
        mRequestStopPlayMusicTs = getNtpTime();

        TXCLog.i(TAG, "sendStopBgmMsg stopTs:" + mRequestStopPlayMusicTs);
        String body = "";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(KEY_CMD, MSG_STOP_CHORUS);
            jsonObject.put(KEY_REQUEST_STOP_TS, mRequestStopPlayMusicTs);
            jsonObject.put(KEY_MUSIC_ID, mMusicID);
            body = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mTRTCCloud.sendCustomCmdMsg(0, body.getBytes(), true, true);
    }

    private void checkMusicProgress() {
        long currentProgress = mTRTCCloud.getAudioEffectManager().getMusicCurrentPosInMS(mMusicID);
        long estimatedProgress = getNtpTime() - mStartPlayMusicTs;
        if (estimatedProgress >= 0 && Math.abs(currentProgress - estimatedProgress) > 60) {
            TXCLog.i(TAG, "checkMusicProgress currentProgress:" + currentProgress + " estimatedProgress:" + estimatedProgress);
            mTRTCCloud.getAudioEffectManager().seekMusicToPosInMS(mMusicID, (int) estimatedProgress);
        }
    }

    @Override
    public void onStart(int id, int errCode) {
        TXCLog.i(TAG, "onStart currentNtp:" + getNtpTime());
        if (errCode < 0) {
            TXCLog.e(TAG, "start play music failed. errCode:" + errCode);
            stopPlayMusic(ChorusStopReason.MusicPlayFailed);
        }
        mListener.onMusicPrepareToPlay(id);
    }

    @Override
    public void onPlayProgress(int id, long curPtsMS, long durationMS) {
        if (mListener != null) {
            mListener.onChorusProgress(id, curPtsMS, durationMS);
        }
        sendMusicPositionMsg();
    }

    @Override
    public void onComplete(int id, int errCode) {
        TXCLog.i(TAG, "onComplete currentNtp:" + getNtpTime());
        if (mListener != null) {
            mListener.onMusicCompletePlaying(id);
        }
        if (errCode < 0) {
            TXCLog.e(TAG, "music play error. errCode:" + errCode);
            stopPlayMusic(ChorusStopReason.MusicPlayFailed);
        } else {
            stopPlayMusic(ChorusStopReason.MusicPlayFinished);
        }
    }

    private void initPusher() {
        if (mPusher != null) {
            return;
        }
        TXCLog.i(TAG, "initPusher");
        try {
            Class<?> clazz = Class.forName(V2TXLIVEPUSHER_PACKAGE_NAME);
            Constructor<?> constructor = clazz.getDeclaredConstructor(Context.class, int.class);
            constructor.setAccessible(true);
            mPusher = (V2TXLivePusher) constructor.newInstance(mContext, 101);
        } catch (Exception e) {
            TXCLog.e(TAG, "initPusher failed : " + e);
            return;
        }
        V2TXLiveDef.V2TXLiveVideoEncoderParam param = new V2TXLiveDef.V2TXLiveVideoEncoderParam(
                V2TXLiveDef.V2TXLiveVideoResolution.V2TXLiveVideoResolution960x540);
        mPusher.setVideoQuality(param);
        mPusher.setAudioQuality(V2TXLiveAudioQualityDefault);
        mPusher.startVirtualCamera(null);
        mPusher.pauseAudio();
        mPusher.setObserver(new V2TXLivePusherObserver() {
            @Override
            public void onPushStatusUpdate(V2TXLiveDef.V2TXLivePushStatus status, String msg, Bundle extraInfo) {
                if (mListener == null) {
                    return;
                }
                CdnPushStatus pushStatus = CdnPushStatus.Disconnected;
                switch (status) {
                    case V2TXLivePushStatusDisconnected:
                        pushStatus = CdnPushStatus.Disconnected;
                        break;
                    case V2TXLivePushStatusConnecting:
                        pushStatus = CdnPushStatus.Connecting;
                        break;
                    case V2TXLivePushStatusReconnecting:
                        pushStatus = CdnPushStatus.Reconnecting;
                        break;
                    case V2TXLivePushStatusConnectSuccess:
                        pushStatus = CdnPushStatus.ConnectSuccess;
                        break;
                    default:
                        TXCLog.i(TAG, "initPusher status : " + status);
                        break;
                }
                mListener.onCdnPushStatusUpdate(pushStatus);
            }
        });
    }

    private void initPlayer() {
        if (mPlayer != null) {
            return;
        }
        TXCLog.i(TAG, "initPlayer");
        mPlayer = new V2TXLivePlayerImpl(mContext);
        mPlayer.enableReceiveSeiMessage(true, SEI_PAYLOAD_TYPE);
        mPlayer.setObserver(new V2TXLivePlayerObserver() {
            @Override
            public void onAudioPlaying(V2TXLivePlayer player, boolean firstPlay, Bundle extraInfo) {
                if (mListener != null) {
                    mListener.onCdnPlayStatusUpdate(CdnPlayStatus.Playing);
                }
            }

            @Override
            public void onAudioLoading(V2TXLivePlayer player, Bundle extraInfo) {
                if (mListener != null) {
                    mListener.onCdnPlayStatusUpdate(CdnPlayStatus.Loading);
                }
            }

            @Override
            public void onError(V2TXLivePlayer player, int code, String msg, Bundle extraInfo) {
                TXCLog.i(TAG, "onError: code = " + code + " , msg = " + msg);
                if (code == V2TXLIVE_ERROR_DISCONNECTED && mListener != null) {
                    mListener.onCdnPlayStatusUpdate(CdnPlayStatus.Stopped);
                }
            }

            @Override
            public void onReceiveSeiMessage(V2TXLivePlayer player, int payloadType, byte[] data) {
                if (data == null || data.length <= 0) {
                    return;
                }
                try {
                    JSONObject json = new JSONObject(new String(data, "UTF-8"));
                    if (!json.has(KEY_MUSIC_CURRENT_TS)) {
                        return;
                    }
                    long position = json.getLong(KEY_MUSIC_CURRENT_TS);
                    mMusicID = Integer.parseInt(json.getString(KEY_MUSIC_CURRENT_ID));
                    if (mListener != null) {
                        mListener.onChorusProgress(mMusicID, position + SEI_LRC_OFFSET, mMusicDuration);
                    }
                } catch (Exception e) {
                    TXCLog.e(TAG, "parse sei message failed. " + e);
                }
            }
        });
    }

    private void sendMusicPositionMsg() {
        if (mPusher == null) {
            return;
        }
        if (mPusher.isPushing() != 1) {
            TXCLog.d(TAG, "you are not pushing, can not send position message");
            return;
        }
        String body = "";
        try {
            JSONObject jsonObject = new JSONObject();
            long currentPosInMs = mTRTCCloud.getAudioEffectManager().getMusicCurrentPosInMS(mMusicID);
            jsonObject.put(KEY_MUSIC_CURRENT_TS, currentPosInMs > 0 ? currentPosInMs : 0);
            jsonObject.put(KEY_MUSIC_CURRENT_ID, mMusicID);
            body = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mPusher.sendSeiMessage(SEI_PAYLOAD_TYPE, body.getBytes());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        mWorkThread.quit();
    }

    /**
     * 合唱回调
     */
    public interface TRTCChorusListener {
        /**
         * 合唱开始回调
         *
         * @param reason 合唱开始原因，参考 {@link ChorusStartReason}
         */
        void onChorusStart(ChorusStartReason reason);

        /**
         * 合唱进度回调
         *
         * @param curPtsMS   合唱音乐当前播放进度，单位：毫秒
         * @param durationMS 合唱音乐总时长，单位：毫秒
         */
        void onChorusProgress(int musicId, long curPtsMS, long durationMS);

        /**
         * 合唱结束回调
         *
         * @param reason 合唱结束原因，参考 {@link ChorusStopReason}
         */
        void onChorusStop(ChorusStopReason reason);

        /**
         * 合唱 CDN 推流连接状态状态改变回调
         *
         * @param status 连接状态
         * @note 此回调透传 V2TXLivePusherObserver onPushStatusUpdate 回调
         */
        void onCdnPushStatusUpdate(CdnPushStatus status);

        /**
         * 合唱 CDN 播放状态改变回调
         *
         * @param status 播放状态
         * @note 此回调透传 V2TXLivePlayerObserver onAudioPlayStatusUpdate 回调
         */
        void onCdnPlayStatusUpdate(CdnPlayStatus status);

        /**
         * 准备播放音乐回调
         *
         * @param musicID 准备播放音乐的ID
         * @note 监听此回调用来更新歌词显示UI
         */
        void onMusicPrepareToPlay(int musicID);

        /**
         * 音乐播放结束回调
         *
         * @param musicID 结束播放的音乐ID
         * @note 监听此回调用来更新歌词显示UI
         */
        void onMusicCompletePlaying(int musicID);

        /**
         * 接收到房主的合唱请求
         *
         * @param musicID    播放时传入的 music ID
         * @param startDelay 开始播放的延时
         */
        void onReceiveAnchorSendChorusMsg(String musicID, long startDelay);
    }

    /**
     * 合唱开始原因
     */
    public enum ChorusStartReason {
        // 本地用户发起合唱
        LocalStart,
        // 远端某个用户发起合唱
        RemoteStart
    }

    /**
     * 合唱结束原因
     */
    public enum ChorusStopReason {
        // 合唱歌曲播放完毕，自动停止
        MusicPlayFinished,
        // 合唱音乐起播失败，被迫终止
        MusicPlayFailed,
        // 本地用户停止合唱
        LocalStop,
        // 远端某个用户请求停止合唱
        RemoteStop
    }

    /**
     * CDN 推流连接状态
     */
    public enum CdnPushStatus {
        // 与服务器断开连接
        Disconnected,
        // 正在连接服务器
        Connecting,
        // 连接服务器成功
        ConnectSuccess,
        // 重连服务器中
        Reconnecting
    }

    /**
     * CDN 播放状态。
     */
    public enum CdnPlayStatus {
        // 播放停止
        Stopped,
        // 正在播放
        Playing,
        // 正在缓冲
        Loading
    }
}
