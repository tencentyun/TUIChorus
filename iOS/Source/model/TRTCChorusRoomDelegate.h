//
//  TRTCChorusRoomDelegate.h
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

#ifndef TRTCChorusRoomDelegate_h
#define TRTCChorusRoomDelegate_h

#import "TRTCChorusRoomDef.h"

NS_ASSUME_NONNULL_BEGIN

@class TRTCVolumeInfo;
@class TRTCQualityInfo;

@protocol TRTCChorusRoomDelegate <NSObject>

/// 错误回调
/// @param code 错误码
/// @param message 错误信息
- (void)onError:(int)code
                message:(NSString*)message
NS_SWIFT_NAME(onError(code:message:));

/// 警告回调
/// @param code 警告码
/// @param message 警告信息
- (void)onWarning:(int)code
                  message:(NSString *)message
NS_SWIFT_NAME(onWarning(code:message:));

/// Debug日志
/// @param message 信息
- (void)onDebugLog:(NSString *)message
NS_SWIFT_NAME(onDebugLog(message:));

/// 网络质量改变回调
/// @param localQuality 本地网络质量
/// @param remoteQuality 远端网络质量
- (void)onNetworkQuality:(TRTCQualityInfo *)localQuality
           remoteQuality:(NSArray<TRTCQualityInfo *> *)remoteQuality
NS_SWIFT_NAME(onNetworkQuality(local:remote:));

/// 房间销毁回调
/// @param message 销毁信息
- (void)onRoomDestroy:(NSString *)message
NS_SWIFT_NAME(onRoomDestroy(message:));

/// 房间信息变更回调
/// @param roomInfo 房间信息
- (void)onRoomInfoChange:(ChorusRoomInfo *)roomInfo
NS_SWIFT_NAME(onRoomInfoChange(roomInfo:));

/// 房间座位变更回调
/// @param seatInfolist 座位列表信息
- (void)onSeatInfoChange:(NSArray<ChorusSeatInfo *> *)seatInfolist
NS_SWIFT_NAME(onSeatListChange(seatInfoList:));

/// 座位关闭回调
/// @param index 座位号
/// @param isClose 是否关闭
- (void)onSeatClose:(NSInteger)index
            isClose:(BOOL)isClose
NS_SWIFT_NAME(onSeatClose(index:isClose:));

/// 主播上麦回调
/// @param index 麦位号
/// @param user 用户信息
- (void)onAnchorEnterSeat:(NSInteger)index
                              user:(ChorusUserInfo *)user
NS_SWIFT_NAME(onAnchorEnterSeat(index:user:));

/// 主播下麦回调
/// @param index 麦位号
/// @param user 用户信息
- (void)onAnchorLeaveSeat:(NSInteger)index
                     user:(ChorusUserInfo *)user
NS_SWIFT_NAME(onAnchorLeaveSeat(index:user:));

/// 用户麦克风是否静音回调
/// @param userId 用户id
/// @param mute 是否静音
- (void)onUserMicrophoneMute:(NSString *)userId mute:(BOOL)mute
NS_SWIFT_NAME(onUserMicrophoneMute(userId:mute:));

/// 观众进房回调
/// @param userInfo 观众信息
- (void)onAudienceEnter:(ChorusUserInfo *)userInfo
NS_SWIFT_NAME(onAudienceEnter(userInfo:));

/// 观众退房回调
/// @param userInfo 观众信息
- (void)onAudienceExit:(ChorusUserInfo *)userInfo
NS_SWIFT_NAME(onAudienceExit(userInfo:));

/// 上麦成员的音量变化
/// @param userVolumes 各个用户音量信息
/// @param totalVolume 整体音量信息
- (void)onUserVolumeUpdate:(NSArray<TRTCVolumeInfo *> *)userVolumes totalVolume:(NSInteger)totalVolume
NS_SWIFT_NAME(onUserVolumeUpdate(userVolumes:totalVolume:));

/// 文本消息接收回调
/// @param message 消息内容
/// @param userInfo 消息发送方信息
- (void)onRecvRoomTextMsg:(NSString *)message
                 userInfo:(ChorusUserInfo *)userInfo
NS_SWIFT_NAME(onRecvRoomTextMsg(message:userInfo:));

/// 自定义消息（信令消息）接收回调
/// @param cmd 信令
/// @param message 消息内容
/// @param userInfo 发送方信息
- (void)onRecvRoomCustomMsg:(NSString *)cmd
                    message:(NSString *)message
                   userInfo:(ChorusUserInfo *)userInfo
NS_SWIFT_NAME(onRecvRoomCustomMsg(cmd:message:userInfo:));

/// 邀请信息接收回调
/// @param identifier 目标用户ID
/// @param inviter 邀请者ID
/// @param cmd 信令
/// @param content 内容
- (void)onReceiveNewInvitation:(NSString *)identifier
                       inviter:(NSString *)inviter
                           cmd:(NSString *)cmd
                       content:(NSString *)content
NS_SWIFT_NAME(onReceiveNewInvitation(identifier:inviter:cmd:content:));

/// 邀请被接受回调
/// @param identifier 目标用户ID
/// @param invitee 邀请者ID
- (void)onInviteeAccepted:(NSString *)identifier
                  invitee:(NSString *)invitee
NS_SWIFT_NAME(onInviteeAccepted(identifier:invitee:));

/// 邀请被拒绝回调
/// @param identifier 目标用户ID
/// @param invitee 邀请者ID
- (void)onInviteeRejected:(NSString *)identifier
                  invitee:(NSString *)invitee
NS_SWIFT_NAME(onInviteeRejected(identifier:invitee:));

/// 邀请被取消回调
/// @param identifier 目标用户ID
/// @param invitee 邀请者ID
- (void)onInvitationCancelled:(NSString *)identifier
                      invitee:(NSString *)invitee NS_SWIFT_NAME(onInvitationCancelled(identifier:invitee:));

/// 歌曲播放进度的回调
/// @param musicID 播放时传入的 music ID
/// @param progress 当前播放时间 / ms
/// @param total 总时间 / ms
- (void)onMusicProgressUpdate:(int32_t)musicID
                     progress:(NSInteger)progress total:(NSInteger)total
NS_SWIFT_NAME(onMusicProgressUpdate(musicID:progress:total:));

/// 准备播放音乐的回调
/// @param musicID 播放时传入的 music ID
- (void)onMusicPrepareToPlay:(int32_t)musicID
NS_SWIFT_NAME(onMusicPrepareToPlay(musicID:));

/// 播放完成音乐的回调
/// @param musicID 播放时传入的 music ID
- (void)onMusicCompletePlaying:(int32_t)musicID
NS_SWIFT_NAME(onMusicCompletePlaying(musicID:));

/// 接收到房主发送的合唱消息回调
/// @param musicId    合唱的歌曲 music ID
/// @param startDelay 合唱的歌曲 延迟多少秒播放
- (void)onReceiveAnchorSendChorusMsg:(NSString *)musicId startDelay:(NSInteger)startDelay
NS_SWIFT_NAME(onReceiveAnchorSendChorusMsg(musicId:startDelay:));

@end

NS_ASSUME_NONNULL_END


#endif /* TRTCChorusDelegate_h */



