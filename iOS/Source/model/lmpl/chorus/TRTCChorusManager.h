//
//  TRTCChorusManager.h
//  TUIChorus
//
//  Created by adams on 2021/8/17.
//  Copyright © 2022 Tencent. All rights reserved.

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, ChorusStartReason) {
    // 本地用户发起合唱（主播端）
    ChorusStartReasonLocal  = 0,
    // 远端某个用户发起合唱（上麦观众）
    ChorusStartReasonRemote = 1,
};

typedef NS_ENUM(NSInteger, ChorusStopReason) {
    // 合唱音乐起播失败，被迫终止
    ChorusStopReasonMusicFailed = 0,
    // 远端某个用户请求停止合唱（上麦观众）
    ChorusStopReasonRemote = 1,
    // 本地用户停止合唱（主播端）
    ChorusStopReasonLocal = 2,
    // 合唱歌曲播放完毕，自动停止
    ChorusStopReasonMusicFinished = 3,
};

typedef NS_ENUM(NSInteger, CdnPushStatus) {
    // 与服务器断开连接
    CdnPushStatusDisconnected = 0,
    // 正在连接服务器
    CdnPushStatusConnecting = 1,
    // 连接服务器成功
    CdnPushStatusConnectSuccess = 2,
    // 重连服务器中
    CdnPushStatusReconnecting = 3,
};

typedef NS_ENUM(NSInteger, CdnPlayStatus) {
    // 播放停止
    CdnPlayStatusStopped = 0,
    // 正在播放
    CdnPlayStatusPlaying = 1,
    // 正在缓冲
    CdnPlayStatusLoading = 2,
};

@protocol TRTCChorusManagerDelegate <NSObject>

/**
 * 合唱已开始
 * 您可以监听这个接口来处理 UI 和业务逻辑
 */
- (void)onChorusStart:(ChorusStartReason)reason message:(NSString *)msg;

/**
 * 合唱已停止
 * 您可以监听这个接口来处理 UI 和业务逻辑
 */
- (void)onChorusStop:(ChorusStopReason)reason message:(NSString *)msg;

/**
 * 准备播放音乐的回调
 * @param musicID 准备播放的音乐ID
 * @note 监听此回调用来更新歌词显示UI
 */
- (void)onMusicPrepareToPlay:(int32_t)musicID;

/**
 * 音乐播放结束的回调
 * @param musicID 准备播放的音乐ID
 * @note 监听此回调用来结束显示正在播放的歌词UI
 */
- (void)onMusicCompletePlaying:(int32_t)musicID;

/**
 * 合唱音乐进度回调
 * 您可以监听这个接口来处理进度条和歌词的滚动
 */
- (void)onMusicProgressUpdate:(int32_t)musicID progress:(NSInteger)progress duration:(NSInteger)durationMS;

/**
 * 合唱 CDN 推流连接状态状态改变回调
 * @param status 连接状态
 * @note 此回调透传 V2TXLivePusherObserver onPushStatusUpdate 回调
 */
- (void)onCdnPushStatusUpdate:(CdnPushStatus)status;

/**
 * 合唱 CDN 播放状态改变回调
 * @param status 播放状态
 * @note 此回调透传 V2TXLivePlayerObserver onAudioPlayStatusUpdate 回调
 */
- (void)onCdnPlayStatusUpdate:(CdnPlayStatus)status;

/**
 * 接收到发起合唱的消息的回调
 * @param musicID    合唱的歌曲ID
 * @param startDelay 合唱的歌曲延迟秒数
 * @note 此回调将musicId回传出去用来对接曲库查询歌曲信息并调用歌曲播放接口
 */
- (void)onReceiveAnchorSendChorusMsg:(NSString *)musicID startDelay:(NSInteger)startDelay;

@end

@interface TRTCChorusManager : NSObject
@property (nonatomic, weak) id<TRTCChorusManagerDelegate> delegate;
@property (nonatomic, assign, readonly) BOOL isChorusOn;    ///是否在合唱中
@property (nonatomic, assign, readonly) BOOL isCdnPushing;  ///是否推流中
@property (nonatomic, assign, readonly) BOOL isCdnPlaying;  ///是否拉流中

/// 初始化方法
- (instancetype)init;

/**
 * 开始合唱
 * 调用后，会收到 onChorusStart 回调，并且房间内的远端用户也会开始合唱
 * @param musicId 歌曲ID
 * @param url 歌曲url
 * @param reason 开始合唱的身份
 * @note 中途加入的用户也会一并开始合唱，音乐进度会与其它用户自动对齐
 */
- (BOOL)startChorus:(NSString *)musicId url:(NSString *)url reason:(ChorusStartReason)reason;

/**
 * 停止合唱
 * 调用后，会收到 onChorusStop 回调，并且房间内的远端用户也会停止合唱
 */
- (void)stopChorus;

/**
 * 开始合唱 CDN 推流
 *
 * @param url 推流地址
 * @return YES：推流成功；NO：推流失败
 */
- (BOOL)startCdnPush:(NSString *)url;

/**
 * 停止合唱 CDN 推流
 */
- (void)stopCdnPush;

/**
 * 开始合唱 CDN 播放
 *
 * @param url  拉流地址
 * @param view 承载视频的 view
 * @return YES：拉流成功；NO：拉流失败
 */
- (BOOL)startCdnPlay:(NSString *)url view:(UIView *_Nullable)view;

/**
 * 停止合唱 CDN 播放
 */
- (void)stopCdnPlay;


/**
 * 接收TRTCCloudDelegate回调消息
 * @param userId 用户ID
 * @param cmdID 命令ID
 * @param seq   消息序号
 * @param message 消息数据
 */
- (void)onRecvCustomCmdMsgUserId:(NSString *)userId cmdID:(NSInteger)cmdID seq:(UInt32)seq message:(NSData *)message;

@end

NS_ASSUME_NONNULL_END
