package com.tencent.liteav.tuichorus.model.impl.trtc;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.tencent.liteav.audio.TXAudioEffectManager;
import com.tencent.liteav.beauty.TXBeautyManager;
import com.tencent.liteav.tuichorus.model.impl.base.TRTCLogger;
import com.tencent.liteav.tuichorus.model.impl.base.TXCallback;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class TRTCChorusRoomService extends TRTCCloudListener {
    private static final String TAG = "TRTCChorusRoomService";

    private static TRTCChorusRoomService sInstance;

    private TRTCCloud                     mTRTCCloud;
    private TXBeautyManager               mTXBeautyManager;
    private boolean                       mIsInRoom;
    private TRTCChorusRoomServiceDelegate mDelegate;
    private String                        mUserId;
    private String                        mRoomId;
    private TRTCCloudDef.TRTCParams       mTRTCParams;
    private Handler                       mMainHandler;
    private TXCallback                    mEnterRoomCallback;
    private TXCallback                    mExitRoomCallback;

    public static synchronized TRTCChorusRoomService getInstance() {
        if (sInstance == null) {
            sInstance = new TRTCChorusRoomService();
        }
        return sInstance;
    }

    public void init(Context context) {
        TRTCLogger.i(TAG, "init context:" + context);
        mTRTCCloud = TRTCCloud.sharedInstance(context);
        mTXBeautyManager = mTRTCCloud.getBeautyManager();
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public void setDelegate(TRTCChorusRoomServiceDelegate delegate) {
        TRTCLogger.i(TAG, "setDelegate:" + delegate);
        mDelegate = delegate;
    }

    public void enterRoom(int sdkAppId, String roomId, String userId, String userSign, int role, TXCallback callback) {
        if (sdkAppId == 0 || TextUtils.isEmpty(roomId) || TextUtils.isEmpty(userId) || TextUtils.isEmpty(userSign)) {
            // 参数非法，可能执行了退房，或者登出
            TRTCLogger.e(TAG, "enter trtc room fail. params invalid. roomId:" + roomId +
                    " userId:" + userId + " sign is empty:" + TextUtils.isEmpty(userSign));
            if (callback != null) {
                callback.onCallback(-1, "enter trtc room fail. params invalid. room id:" +
                        roomId + " user id:" + userId + " sign is empty:" + TextUtils.isEmpty(userSign));
            }
            return;
        }
        mUserId = userId;
        mRoomId = roomId;
        mEnterRoomCallback = callback;
        TRTCLogger.i(TAG, "enter room, sdkAppId:" + sdkAppId + " roomId:" + roomId + " userId:" +
                userId + " sign:" + TextUtils.isEmpty(userId));
        mTRTCParams = new TRTCCloudDef.TRTCParams();
        mTRTCParams.sdkAppId = sdkAppId;
        mTRTCParams.userId = userId;
        mTRTCParams.userSig = userSign;
        mTRTCParams.role = role;
        // 字符串房间号逻辑
        mTRTCParams.roomId = Integer.valueOf(roomId);
        internalEnterRoom();
    }

    private void setFramework(int framework) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("api", "setFramework");
            JSONObject params = new JSONObject();
            params.put("framework", framework);
            jsonObject.put("params", params);
            mTRTCCloud.callExperimentalAPI(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void internalEnterRoom() {
        // 进房前设置一下监听，不然可能会被其他信息打断
        if (mTRTCParams == null) {
            return;
        }
        setFramework(5);
        mTRTCCloud.setListener(this);
        mTRTCCloud.enterRoom(mTRTCParams, TRTCCloudDef.TRTC_APP_SCENE_VOICE_CHATROOM);
        // enable volume callback
        enableAudioEvaluation(true);
    }

    public void exitRoom(TXCallback callback) {
        TRTCLogger.i(TAG, "exit room.");
        mUserId = null;
        mTRTCParams = null;
        mEnterRoomCallback = null;
        mExitRoomCallback = callback;
        mMainHandler.removeCallbacksAndMessages(null);
        mTRTCCloud.exitRoom();
    }

    public void muteLocalAudio(boolean mute) {
        TRTCLogger.i(TAG, "muteLocalAudio mute:" + mute);
        mTRTCCloud.muteLocalAudio(mute);
    }

    public void muteRemoteAudio(String userId, boolean mute) {
        TRTCLogger.i(TAG, "muteRemoteAudio userId:" + userId + " , mute:" + mute);
        mTRTCCloud.muteRemoteAudio(userId, mute);
    }

    public void muteAllRemoteAudio(boolean mute) {
        TRTCLogger.i(TAG, "muteAllRemoteAudio mute:" + mute);
        mTRTCCloud.muteAllRemoteAudio(mute);
    }

    public boolean isEnterRoom() {
        return mIsInRoom;
    }

    @Override
    public void onEnterRoom(long l) {
        TRTCLogger.i(TAG, "onEnterRoom result:" + l);
        if (mEnterRoomCallback != null) {
            if (l > 0) {
                mIsInRoom = true;
                mEnterRoomCallback.onCallback(0, "enter room success.");
            } else {
                mIsInRoom = false;
                mEnterRoomCallback.onCallback((int) l, "enter room fail");
            }
        }
    }

    @Override
    public void onExitRoom(int result) {
        TRTCLogger.i(TAG, "onExitRoom result : " + result);
        if (mExitRoomCallback != null) {
            mIsInRoom = false;
            mExitRoomCallback.onCallback(0, "exit room success.");
            mExitRoomCallback = null;
        }
    }

    @Override
    public void onRemoteUserEnterRoom(String userId) {
        TRTCLogger.i(TAG, "onRemoteUserEnterRoom userId:" + userId);
        if (mDelegate != null) {
            mDelegate.onTRTCAnchorEnter(userId);
        }
    }

    @Override
    public void onRemoteUserLeaveRoom(String userId, int i) {
        TRTCLogger.i(TAG, "onRemoteUserLeaveRoom userId:" + userId);
        if (mDelegate != null) {
            mDelegate.onTRTCAnchorExit(userId);
        }
    }

    @Override
    public void onUserVideoAvailable(String userId, boolean available) {
        TRTCLogger.i(TAG, "onUserVideoAvailable userId:" + userId + " available:" + available);
        if (mDelegate != null) {
            mDelegate.onTRTCVideoAvailable(userId, available);
        }
    }

    @Override
    public void onUserAudioAvailable(String userId, boolean available) {
        TRTCLogger.i(TAG, "onUserAudioAvailable userId:" + userId + " available:" + available);
        if (mDelegate != null) {
            mDelegate.onTRTCAudioAvailable(userId, available);
        }
    }

    @Override
    public void onError(int errorCode, String errorMsg, Bundle bundle) {
        TRTCLogger.i(TAG, "onError: " + errorCode);
        if (mDelegate != null) {
            mDelegate.onError(errorCode, errorMsg);
        }
    }

    @Override
    public void onNetworkQuality(final TRTCCloudDef.TRTCQuality trtcQuality, final ArrayList<TRTCCloudDef.TRTCQuality> arrayList) {
        if (mDelegate != null) {
            mDelegate.onNetworkQuality(trtcQuality, arrayList);
        }
    }

    @Override
    public void onUserVoiceVolume(final ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {
        if (mDelegate != null && userVolumes.size() != 0) {
            mDelegate.onUserVoiceVolume(userVolumes, totalVolume);
        }
    }

    @Override
    public void onSetMixTranscodingConfig(int i, String s) {
        super.onSetMixTranscodingConfig(i, s);
        TRTCLogger.i(TAG, "onSetMixTranscodingConfig code: " + i + " msg:" + s);
    }

    public TXBeautyManager getTXBeautyManager() {
        return mTXBeautyManager;
    }

    public void setAudioQuality(int quality) {
        mTRTCCloud.setAudioQuality(quality);
    }

    public void startMicrophone() {
        mTRTCCloud.startLocalAudio();
    }

    public void enableAudioEarMonitoring(boolean enable) {
        mTRTCCloud.enableAudioEarMonitoring(enable);
    }

    public void switchToAnchor() {
        mTRTCCloud.switchRole(TRTCCloudDef.TRTCRoleAnchor);
        mTRTCCloud.startLocalAudio();
    }

    public void switchToAudience() {
        mTRTCCloud.stopLocalAudio();
        mTRTCCloud.switchRole(TRTCCloudDef.TRTCRoleAudience);
    }

    public void stopMicrophone() {
        mTRTCCloud.stopLocalAudio();
    }

    public void setSpeaker(boolean useSpeaker) {
        mTRTCCloud.setAudioRoute(useSpeaker ? TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER : TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE);
    }

    public void setAudioCaptureVolume(int volume) {
        mTRTCCloud.setAudioCaptureVolume(volume);
    }

    public void setAudioPlayoutVolume(int volume) {
        mTRTCCloud.setAudioPlayoutVolume(volume);
    }

    public void startFileDumping(TRTCCloudDef.TRTCAudioRecordingParams trtcAudioRecordingParams) {
        mTRTCCloud.startAudioRecording(trtcAudioRecordingParams);
    }

    public void stopFileDumping() {
        mTRTCCloud.stopAudioRecording();
    }

    public void enableAudioEvaluation(boolean enable) {
        mTRTCCloud.enableAudioVolumeEvaluation(enable ? 300 : 0);
    }

    public TXAudioEffectManager getAudioEffectManager() {
        return mTRTCCloud.getAudioEffectManager();
    }

    public boolean sendSEIMsg(byte[] data, int repeatCount) {
        return mTRTCCloud.sendSEIMsg(data, repeatCount);
    }

    @Override
    public void onRecvSEIMsg(String userId, byte[] data) {

    }

    public void startRemoteView(String userId, TXCloudVideoView view) {
        mTRTCCloud.startRemoteView(userId, view);
    }

    public void stopRemoteView(String userId) {
        mTRTCCloud.stopRemoteView(userId);
    }

    public boolean sendCustomCmdMsg(int cmdID, byte[] data, boolean reliable, boolean ordered) {
        return mTRTCCloud.sendCustomCmdMsg(cmdID, data, reliable, ordered);
    }

    @Override
    public void onRecvCustomCmdMsg(String userId, int cmdID, int seq, byte[] message) {
        if (mDelegate != null && message != null) {
            mDelegate.onRecvCustomCmdMsg(userId, cmdID, seq, message);
        }
    }

    public void enableRealtimeChorus(int status) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("api", "enableRealtimeChorus");
            JSONObject params = new JSONObject();
            params.put("enable", status);
            jsonObject.put("params", params);
            mTRTCCloud.callExperimentalAPI(String.format(Locale.ENGLISH, jsonObject.toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
