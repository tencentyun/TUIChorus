package com.tencent.liteav.tuichorus.model;

import com.tencent.trtc.TRTCCloudDef;

import java.util.List;

public interface TRTCChorusRoomDelegate {
    /**
     * 组件出错信息，请务必监听并处理
     */
    void onError(int code, String message);
    
    /**
     * 组件告警信息
     */
    void onWarning(int code, String message);
    
    /**
     * 组件log信息
     */
    void onDebugLog(String message);
    
    /**
     * 房间被销毁，当主播调用destroyRoom后，听众会收到该回调
     */
    void onRoomDestroy(String roomId);
    
    /**
     * 房间信息改变的通知
     */
    void onRoomInfoChange(TRTCChorusRoomDef.RoomInfo roomInfo);
    
    /**
     * 全量的麦位列表变化,包含了整个麦位表
     *
     * @param seatInfoList 全量的麦位列表
     */
    void onSeatListChange(List<TRTCChorusRoomDef.SeatInfo> seatInfoList);
    
    /**
     * 有成员上麦(主动上麦/主播抱人上麦)
     *
     * @param index 上麦的麦位
     * @param user  用户详细信息
     */
    void onAnchorEnterSeat(int index, TRTCChorusRoomDef.UserInfo user);
    
    /**
     * 有成员下麦(主动下麦/主播踢人下麦)
     *
     * @param index 下麦的麦位
     * @param user  用户详细信息
     */
    void onAnchorLeaveSeat(int index, TRTCChorusRoomDef.UserInfo user);
    
    /**
     * 主播禁麦
     *
     * @param index  操作的麦位
     * @param isMute 是否静音
     */
    void onSeatMute(int index, boolean isMute);
    
    /**
     * 用户麦克风是否静音
     *
     * @param userId 用户id
     * @param mute   是否静音
     */
    void onUserMicrophoneMute(String userId, boolean mute);
    
    /**
     * 主播封麦
     *
     * @param index   操作的麦位
     * @param isClose 是否封禁麦位
     */
    void onSeatClose(int index, boolean isClose);
    
    /**
     * 听众进入房间
     *
     * @param userInfo 听众的详细信息
     */
    void onAudienceEnter(TRTCChorusRoomDef.UserInfo userInfo);
    
    /**
     * 听众离开房间
     *
     * @param userInfo 听众的详细信息
     */
    void onAudienceExit(TRTCChorusRoomDef.UserInfo userInfo);
    
    /**
     * 网络状态回调。
     *
     * @param localQuality  上行网络质量。
     * @param remoteQuality 下行网络质量。
     */
    void onNetworkQuality(TRTCCloudDef.TRTCQuality localQuality, List<TRTCCloudDef.TRTCQuality> remoteQuality);
    
    /**
     * 上麦成员的音量变化
     *
     * @param userVolumes 用户列表
     * @param totalVolume 音量大小 0-100
     */
    void onUserVolumeUpdate(List<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume);
    
    /**
     * 收到文本消息。
     *
     * @param message  文本消息。
     * @param userInfo 发送者用户信息。
     */
    void onRecvRoomTextMsg(String message, TRTCChorusRoomDef.UserInfo userInfo);
    
    /**
     * 收到自定义消息。
     *
     * @param cmd      命令字，由开发者自定义，主要用于区分不同消息类型。
     * @param message  文本消息。
     * @param userInfo 发送者用户信息。
     */
    void onRecvRoomCustomMsg(String cmd, String message, TRTCChorusRoomDef.UserInfo userInfo);
    
    /**
     * 收到新的邀请请求
     *
     * @param id      邀请id
     * @param inviter 邀请人userId
     * @param cmd     业务指定的命令字
     * @param content 业务指定的内容
     */
    void onReceiveNewInvitation(String id, String inviter, String cmd, String content);
    
    /**
     * 被邀请者接受邀请
     *
     * @param id      邀请id
     * @param invitee 被邀请人userId
     */
    void onInviteeAccepted(String id, String invitee);
    
    /**
     * 被邀请者拒绝邀请
     *
     * @param id      邀请id
     * @param invitee 被邀请人userId
     */
    void onInviteeRejected(String id, String invitee);
    
    /**
     * 邀请人取消邀请
     *
     * @param id      邀请id
     * @param inviter 邀请人userId
     */
    void onInvitationCancelled(String id, String inviter);
    
    /**
     * 歌曲播放进度的回调
     *
     * @param musicID  播放时传入的 music ID
     * @param progress 当前播放时间 / ms
     * @param total    总时间 / ms
     */
    void onMusicProgressUpdate(int musicID, long progress, long total);
    
    /**
     * 准备播放音乐的回调
     *
     * @param musicID 播放时传入的 music ID
     */
    void onMusicPrepareToPlay(int musicID);
    
    /**
     * 播放完成音乐的回调
     *
     * @param musicID 播放时传入的 music ID
     */
    void onMusicCompletePlaying(int musicID);
    
    /**
     * 接收到房主的合唱请求
     *
     * @param musicID 播放时传入的 music ID
     * @param startDelay 开始播放的延时
     */
    void onReceiveAnchorSendChorusMsg(String musicID, long startDelay);
}
