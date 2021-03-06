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
     * ??????????????????
     */
    private          String  mMusicPath;
    private          int     mMusicID;
    private volatile long    mMusicDuration;
    private volatile boolean mIsChorusOn;
    private          long    mRevStartPlayMusicTs;
    private volatile long    mStartPlayMusicTs;
    private          long    mRequestStopPlayMusicTs;

    private ChorusStartReason mChorusStartReason = ChorusStartReason.LocalStart;//?????????????????????????????????????????????????????????:??????/??????

    /**
     * ?????? cdn ??????
     */
    private              V2TXLivePusher mPusher;
    private              V2TXLivePlayer mPlayer;
    private static final int            SEI_PAYLOAD_TYPE = 242;
    private static final int            SEI_LRC_OFFSET   = -100; //??????????????????????????????

    public TRTCChorusManager(@NonNull Context context, @NonNull TRTCCloud trtcCloud) {
        mContext = context;
        mTRTCCloud = trtcCloud;
        mWorkThread = new HandlerThread("TRTCChorusManagerWorkThread");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper());
        initPusher();
    }

    /**
     * ??????????????????
     *
     * @param listener ????????????
     */
    public void setListener(TRTCChorusListener listener) {
        mListener = listener;
    }

    /**
     * ????????????
     *
     * @return true????????????????????????false?????????????????????
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
     * ????????????
     */
    public void stopChorus() {
        TXCLog.i(TAG, "stopChorus");
        clearStatus();
        stopPlayMusic(ChorusStopReason.LocalStop);
    }

    /**
     * ????????????
     */
    public void clearStatus() {
        mChorusStartReason = ChorusStartReason.LocalStart;
    }

    /**
     * ????????????????????????
     *
     * @return true???????????????????????????false?????????????????????
     */
    public boolean isChorusOn() {
        return mIsChorusOn;
    }

    /**
     * TRTC ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param userId  ????????????
     * @param cmdID   ?????? ID
     * @param seq     ????????????
     * @param message ????????????
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
                        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
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
     * ???????????? CDN ??????
     *
     * @param url ????????????
     * @return true??????????????????false???????????????
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
     * ???????????? CDN ??????
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
     * ???????????? CDN ?????????
     *
     * @return true??????????????????false???????????????
     */
    public boolean isCdnPushing() {
        initPusher();
        return mPusher.isPushing() == 1;
    }

    /**
     * ???????????? CDN ??????
     *
     * @param url  ????????????
     * @param view ??????????????? view
     * @return true??????????????????false???????????????
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
     * ???????????? CDN ??????
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
     * ???????????? CDN ?????????
     *
     * @return true??????????????????false???????????????
     */
    public boolean isCdnPlaying() {
        initPlayer();
        return mPlayer.isPlaying() == 1;
    }

    /////////////////////////////////////////////////////////////////////////////////
    //
    //                    ????????????
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
            //??? delayMs ??????????????????????????????????????????????????????????????????
            //??????????????? delayMs ?????????????????????????????? BGM ?????????????????????????????????????????????????????????????????????
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
                    // ???????????????????????????????????????????????????????????????????????????
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
                    //?????????????????????????????????????????????
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
        //???????????????,??????????????????????????????
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
     * ????????????
     */
    public interface TRTCChorusListener {
        /**
         * ??????????????????
         *
         * @param reason ??????????????????????????? {@link ChorusStartReason}
         */
        void onChorusStart(ChorusStartReason reason);

        /**
         * ??????????????????
         *
         * @param curPtsMS   ????????????????????????????????????????????????
         * @param durationMS ???????????????????????????????????????
         */
        void onChorusProgress(int musicId, long curPtsMS, long durationMS);

        /**
         * ??????????????????
         *
         * @param reason ??????????????????????????? {@link ChorusStopReason}
         */
        void onChorusStop(ChorusStopReason reason);

        /**
         * ?????? CDN ????????????????????????????????????
         *
         * @param status ????????????
         * @note ??????????????? V2TXLivePusherObserver onPushStatusUpdate ??????
         */
        void onCdnPushStatusUpdate(CdnPushStatus status);

        /**
         * ?????? CDN ????????????????????????
         *
         * @param status ????????????
         * @note ??????????????? V2TXLivePlayerObserver onAudioPlayStatusUpdate ??????
         */
        void onCdnPlayStatusUpdate(CdnPlayStatus status);

        /**
         * ????????????????????????
         *
         * @param musicID ?????????????????????ID
         * @note ???????????????????????????????????????UI
         */
        void onMusicPrepareToPlay(int musicID);

        /**
         * ????????????????????????
         *
         * @param musicID ?????????????????????ID
         * @note ???????????????????????????????????????UI
         */
        void onMusicCompletePlaying(int musicID);

        /**
         * ??????????????????????????????
         *
         * @param musicID    ?????????????????? music ID
         * @param startDelay ?????????????????????
         */
        void onReceiveAnchorSendChorusMsg(String musicID, long startDelay);
    }

    /**
     * ??????????????????
     */
    public enum ChorusStartReason {
        // ????????????????????????
        LocalStart,
        // ??????????????????????????????
        RemoteStart
    }

    /**
     * ??????????????????
     */
    public enum ChorusStopReason {
        // ???????????????????????????????????????
        MusicPlayFinished,
        // ???????????????????????????????????????
        MusicPlayFailed,
        // ????????????????????????
        LocalStop,
        // ????????????????????????????????????
        RemoteStop
    }

    /**
     * CDN ??????????????????
     */
    public enum CdnPushStatus {
        // ????????????????????????
        Disconnected,
        // ?????????????????????
        Connecting,
        // ?????????????????????
        ConnectSuccess,
        // ??????????????????
        Reconnecting
    }

    /**
     * CDN ???????????????
     */
    public enum CdnPlayStatus {
        // ????????????
        Stopped,
        // ????????????
        Playing,
        // ????????????
        Loading
    }
}
