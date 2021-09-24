package com.tencent.liteav.tuichorus.model.impl.trtc;

import com.tencent.trtc.TRTCCloudDef;

import java.util.ArrayList;

public interface TRTCChorusRoomServiceDelegate {
    void onTRTCAnchorEnter(String userId);

    void onTRTCAnchorExit(String userId);

    void onTRTCVideoAvailable(String userId, boolean available);

    void onTRTCAudioAvailable(String userId, boolean available);

    void onError(int errorCode, String errorMsg);

    void onNetworkQuality(TRTCCloudDef.TRTCQuality trtcQuality, ArrayList<TRTCCloudDef.TRTCQuality> arrayList);

    void onUserVoiceVolume(ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume);

    void onRecvCustomCmdMsg(String userId, int cmdID, int seq, byte[] message);

}
