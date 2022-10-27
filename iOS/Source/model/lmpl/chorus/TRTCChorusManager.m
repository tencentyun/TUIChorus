//
//  TRTCChorusManager.m
//  TUIChorus
//
//  Created by adams on 2021/8/17.
//  Copyright © 2022 Tencent. All rights reserved.

#import "TRTCChorusManager.h"
#import "TXChorusBaseDef.h"
#import "TXLiveBase.h"
#import "TRTCCloud.h"
#import "V2TXLivePusher.h"
#import "V2TXLivePlayer.h"

//通用宏定义
#define CHORUS_WEAKIFY(x) __weak __typeof(x) weak_##x = x
#define CHORUS_STRONGIFY_OR_RETURN(x) __strong __typeof(weak_##x) x = weak_##x; if (x == nil) {return;};
#define CHORUS_LOG_TAG TRTCChorusManager

//麦上相关
#define KChorusCmd @"cmd"
#define KChorusTimestampPlay @"startPlayMusicTS"
#define KChorusTimestampStop @"requestStopTS"
#define KChorusMusicID @"music_id"
#define KChorusMusicDuration @"music_duration"
#define KChorusCmdStart @"startChorus"
#define KChorusCmdStop @"stopChorus"
#define CHORUS_MUSIC_START_DELAY 3000
#define CHORUS_PRELOAD_MUSIC_DELAY 400

//麦下相关
#define CHORUS_SEI_PAYLOAD_TYPE 242
#define V2TXLIVEMODE_SIMPLE 101
#define KMusicCurrentTs @"musicCurrentTS"
#define KMusicCurrentId @"musicCurrentId"

@interface TRTCCloud(ChorusLog)
// 打印一些合唱的关键log到本地日志中
- (void)apiLog:(NSString*)log;
@end

@interface TRTCChorusManager()<V2TXLivePusherObserver, V2TXLivePlayerObserver, TRTCCloudDelegate>
//合唱麦上相关
@property (nonatomic, assign) NSInteger startPlayChorusMusicTs;
@property (nonatomic, assign) NSInteger requestStopChorusTs;
@property (nonatomic, assign) NSInteger startDelayMs;
@property (nonatomic, assign) NSInteger musicDuration;
@property (nonatomic, strong) NSTimer *chorusLongTermTimer;
@property (nonatomic, strong) dispatch_source_t delayStartChorusMusicTimer;
@property (nonatomic, strong) dispatch_source_t preloadMusicTimer;
@property (nonatomic, strong) TXAudioMusicParam *musicParam;
@property (nonatomic, assign) ChorusStartReason chorusReason;
@property (nonatomic, assign) int32_t currentPlayMusicID;

//合唱cdn相关
@property (nonatomic, strong) V2TXLivePusher *pusher;
@property (nonatomic, strong) V2TXLivePlayer *player;
@end

@implementation TRTCChorusManager
{
    BOOL _isChorusOn;
}
#pragma mark - lazy property
- (NSTimer *)chorusLongTermTimer {
    if (!_chorusLongTermTimer) {
        __weak typeof(self)weakSelf = self;
        _chorusLongTermTimer = [NSTimer scheduledTimerWithTimeInterval:2.0 repeats:YES block:^(NSTimer * _Nonnull timer) {
            __strong typeof(weakSelf)strongSelf = weakSelf;
            if (!strongSelf) {return;}
            [strongSelf checkMusicProgress];
            if (strongSelf.chorusReason == ChorusStartReasonLocal) {
                [strongSelf sendStartChorusMsg];
            }
        }];
    }
    return _chorusLongTermTimer;
}

#pragma mark - 初始化相关
- (instancetype)init {
    self = [super init];
    if (self) {
        [[TRTCCloud sharedInstance] apiLog:@"TRTCChorusManager init"];
        self.chorusReason = ChorusStartReasonLocal;
        self.startPlayChorusMusicTs = -1;
        self.requestStopChorusTs = -1;
        self.musicDuration = -1;
        self.currentPlayMusicID = -1;
        [self.chorusLongTermTimer setFireDate:[NSDate distantFuture]];
        [[self audioEffecManager] setAllMusicVolume:80];
        [TXLiveBase updateNetworkTime];
    }
    return self;
}

- (void)dealloc {
    [[TRTCCloud sharedInstance] apiLog:@"TRTCChorusManager dealloc"];
    [self.chorusLongTermTimer setFireDate:[NSDate distantFuture]];
    [self.chorusLongTermTimer invalidate];
    self.chorusLongTermTimer = nil;
    [self stopChorus];
}

#pragma mark - Public Methods
- (BOOL)startChorus:(NSString *)musicId url:(NSString *)url reason:(ChorusStartReason)reason {
    if (![self isNtpReady]) {
        [[TRTCCloud sharedInstance] apiLog:@"TRTCChorusManager startChorus failed, ntp is not ready, please call [TXLiveBase updateNetworkTime] first!"];
        return NO;
    }
    self.chorusReason = reason;
    self.currentPlayMusicID = [musicId intValue];
    
    NSInteger chorusStartPlayDelay = CHORUS_MUSIC_START_DELAY;
    if (self.chorusReason == ChorusStartReasonLocal) {
        self.startPlayChorusMusicTs = [TXLiveBase getNetworkTimestamp] + CHORUS_MUSIC_START_DELAY;
    } else {
        chorusStartPlayDelay = self.startDelayMs;
    }
    
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager startChorus, schedule time:%ld, current_ntp:%ld", self.startPlayChorusMusicTs, [TXLiveBase getNetworkTimestamp]]];
    
    TXAudioMusicParam *param = [[TXAudioMusicParam alloc] init];
    param.ID = self.currentPlayMusicID;
    param.path = url;
    param.loopCount = 0;
    param.publish = NO;
    self.musicParam = param;
    self.musicDuration = [[self audioEffecManager] getMusicDurationInMS:self.musicParam.path];
    TRTCLog(@"___ chorus: start play: %@", musicId);
    [self.chorusLongTermTimer setFireDate:[NSDate distantPast]];
    [self schedulePlayMusic:chorusStartPlayDelay];
    if (self.chorusReason == ChorusStartReasonLocal) {
        [self sendStartChorusMsg];
    }
    
    // 若成功合唱，通知合唱已开始
    _isChorusOn = YES;
    if (self.chorusReason == ChorusStartReasonLocal) {
        [self asyncDelegate:^{
            if (self.delegate && [self.delegate respondsToSelector:@selector(onChorusStart:message:)]) {
                [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onChorusStart, reason:ChorusStartReasonLocal, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
                [self.delegate onChorusStart:ChorusStartReasonLocal message:@"local user launched chorus"];
            }
        }];
    }
    return YES;
}

- (void)stopChorus {
    if (_isChorusOn) {
        switch (self.chorusReason) {
            case ChorusStartReasonLocal:
                [self stopLocalChorus];
                break;
            case ChorusStartReasonRemote:
                [self stopRemoteChorus];
                break;
            default:
                break;
        }
    }
}

- (BOOL)startCdnPush:(NSString *)url {
    if (!self.pusher) {
        [self initPusher];
    }
    NSString *pushURL = url;
    if (![url hasSuffix:@"&enableblackstream=1"]) {
        pushURL = [url stringByAppendingString:@"&enableblackstream=1"];
    }
    TRTCLog(@"pushURL = %@",pushURL);
    V2TXLiveCode result = [self.pusher startPush:pushURL];
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager startCdnPush finished, result:%ld, current_ntp:%ld", result, [TXLiveBase getNetworkTimestamp]]];
    return result == V2TXLIVE_OK;
    return NO;
}

- (void)stopCdnPush {
    if (!self.pusher) {
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager stopCdnPush failed, pusher is nil, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
    }
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager stopCdnPush, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
    [self.pusher stopPush];
}

- (BOOL)startCdnPlay:(NSString *)url view:(UIView  *_Nullable)view {
    if (!self.player) {
        [self initPlayer];
    }
    if (view) {
        [self.player setRenderView:view];
    }
    V2TXLiveCode result = [self.player startLivePlay:url];
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager startCdnPlay finished, url:%@, view:%p, result:%ld, current_ntp:%ld", url, view, result, [TXLiveBase getNetworkTimestamp]]];
    return result == V2TXLIVE_OK;
    return NO;
}

- (void)stopCdnPlay {
    if (!self.player) {
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager stopCdnPlay failed, player is nil, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
    }
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager stopCdnPlay, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
    [self.player stopPlay];
    self.currentPlayMusicID = -1;
}

- (void)onRecvCustomCmdMsgUserId:(NSString *)userId cmdID:(NSInteger)cmdID seq:(UInt32)seq message:(NSData *)message {
    if (![self isNtpReady]) {//ntp校时为完成，直接返回
        [[TRTCCloud sharedInstance] apiLog:@"TRTCChorusManager ignore command, ntp is not ready"];;
        return;
    }
    
    NSString *msg = [[NSString alloc] initWithData:message encoding:NSUTF8StringEncoding];
    if(msg == nil) {
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager ignore command, userId:%@, msg:%@, current_ntp:%ld", userId, msg, [TXLiveBase getNetworkTimestamp]]];
        return;
    }
    
    NSError *error;
    NSDictionary *json = [NSJSONSerialization JSONObjectWithData:[msg dataUsingEncoding:NSUTF8StringEncoding]
                                                         options:NSJSONReadingMutableContainers
                                                           error:&error];
    if(error) {
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager ignore command, userId:%@, json error:%@, current_ntp:%ld", userId, error, [TXLiveBase getNetworkTimestamp]]];
        return;
    }
    
    NSObject *cmdObj = [json objectForKey:KChorusCmd];
    if(![cmdObj isKindOfClass:[NSString class]]) {
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager ignore command, userId:%@, cmdObj is not a NSString, current_ntp:%ld", userId, [TXLiveBase getNetworkTimestamp]]];
        return;
    }
    
    
    NSString *musicId = [json objectForKey:KChorusMusicID];
    if ([musicId intValue] == 0) {
        TRTCLog(@"%@", [NSString stringWithFormat:@"TRTCChorus ignore command, userId:%@, musicID is zero, current_ntp:%ld", userId, [TXLiveBase getNetworkTimestamp]]);
        return;
    }
    self.musicDuration = [[json objectForKey:KChorusMusicDuration] integerValue];
    
    NSString *cmd = (NSString *)cmdObj;
    if ([cmd isEqualToString:KChorusCmdStart]) {
        NSObject *startPlayMusicTsObj = [json objectForKey:KChorusTimestampPlay];
        if (!startPlayMusicTsObj || (![startPlayMusicTsObj isKindOfClass:[NSNumber class]])){
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager ignore start command, userId:%@, startPlayMusicTS not found, current_ntp:%ld", userId, [TXLiveBase getNetworkTimestamp]]];
            return;
        }
        NSInteger startPlayMusicTs = ((NSNumber *)startPlayMusicTsObj).longLongValue;
        if (startPlayMusicTs < self.requestStopChorusTs) {
            //当前收到的命令是在请求停止合唱之前发出的，需要忽略掉，否则会导致请求停止后又开启了合唱
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:
            @"TRTCChorusManager receive kStartChorusMsg that sent before requesting stop, ignore. userId:%@, startPlayMusicTs:%ld, requestStopChorusTs:%ld, current_ntp:%ld",
            userId, startPlayMusicTs, self.requestStopChorusTs, [TXLiveBase getNetworkTimestamp]]];
            return;
        }
        if (self.isChorusOn == NO) {
            NSInteger startDelayMS = startPlayMusicTs - [TXLiveBase getNetworkTimestamp];
            if (startDelayMS <= -self.musicDuration) {
                //若 delayMs 为负数，代表约定的合唱开始时间在当前时刻之前
                //进一步，若 delayMs 为负，并且绝对值大于 BGM 时长，证明此时合唱已经结束了，应当忽略此次消息
                [self clearChorusState];
                [[TRTCCloud sharedInstance] apiLog: [NSString stringWithFormat:@"TRTCChorusManager ignore command, chorus is over, userId:%@, startPlayMusicTs:%ld current_ntp:%ld", userId, startPlayMusicTs, [TXLiveBase getNetworkTimestamp]]];
                return;
            }
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager schedule time:%ld, delay:%ld, current_ntp:%ld", startPlayMusicTs, startDelayMS, [TXLiveBase getNetworkTimestamp]]];
            //副唱开始合唱后，也发送 kStartChorusMsg 信令，这样若主唱重进房则可恢复合唱进度
            self.startPlayChorusMusicTs = startPlayMusicTs;
            self.startDelayMs = startDelayMS;
            if (self.chorusReason == ChorusStartReasonLocal) {
                self.chorusReason = ChorusStartReasonRemote;
            }
            [self.chorusLongTermTimer setFireDate:[NSDate distantPast]];
            [self asyncDelegate:^{
                if ([self canDelegateResponseMethod:@selector(onReceiveAnchorSendChorusMsg:startDelay:)]) {
                    [self.delegate onReceiveAnchorSendChorusMsg:musicId startDelay:startDelayMS];
                }
            }];
        }
    } else if ([cmd isEqualToString:KChorusCmdStop]) {
        NSObject *requestStopTsObj = [json objectForKey:KChorusTimestampStop];
        if (!requestStopTsObj || (![requestStopTsObj isKindOfClass:[NSNumber class]])) {
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager ignore stop command, requestStopTS not found, userId:%@, current_ntp:%ld", userId, [TXLiveBase getNetworkTimestamp]]];
            return;
        }
        self.requestStopChorusTs = ((NSNumber *)requestStopTsObj).longLongValue;
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager receive stop command, userId:%@, requestStopTS:%ld, current_ntp:%ld", userId, self.requestStopChorusTs, [TXLiveBase getNetworkTimestamp]]];
        if (self.chorusReason == ChorusStartReasonLocal) {
            self.chorusReason = ChorusStartReasonRemote;
        }
        [self stopChorus];
    }
}

#pragma mark - V2TXLivePlayerObserver
- (void)onReceiveSeiMessage:(id<V2TXLivePlayer>)player payloadType:(int)payloadType data:(NSData *)data {
    NSString *msg = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    if(msg == nil) {
        return;
    }
    
    NSError *error;
    NSDictionary *json = [NSJSONSerialization JSONObjectWithData:[msg dataUsingEncoding:NSUTF8StringEncoding]
                                                         options:NSJSONReadingMutableContainers
                                                           error:&error];
    if (!error) {
        NSObject *musicIdObj = [json objectForKey:KMusicCurrentId];
        if (musicIdObj && [musicIdObj isKindOfClass:[NSNumber class]]) {
            int32_t musicId = ((NSNumber *)musicIdObj).intValue;
            if (self.currentPlayMusicID != musicId) {
                self.currentPlayMusicID = musicId;
            }
        } else {
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager onReceiveSeiMessage ignored, music id not found, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
        }
        
        NSObject *progressObj = [json objectForKey:KMusicCurrentTs];
        if (progressObj && [progressObj isKindOfClass:[NSNumber class]]) {
            NSInteger progress = ((NSNumber *)progressObj).integerValue;
            //通知歌曲进度，用户会在这里进行歌词的滚动
            [self asyncDelegate:^{
                if (self.delegate && [self.delegate respondsToSelector:@selector(onMusicProgressUpdate:progress:duration:)]) {
                    [self.delegate onMusicProgressUpdate:self.currentPlayMusicID progress:progress duration:self.musicDuration];
                }
            }];
        } else {
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager onReceiveSeiMessage ignored, music progress not found, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
        }
    } else {
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager onReceiveSeiMessage ignored, JSONObjectWithData error:%@, current_ntp:%ld", error.localizedDescription, [TXLiveBase getNetworkTimestamp]]];
    }
}

- (void)onAudioLoading:(id<V2TXLivePlayer>)player extraInfo:(NSDictionary *)extraInfo {
    NSLog(@"----- onAudioLoading");
    if (self.delegate && [self.delegate respondsToSelector:@selector(onCdnPlayStatusUpdate:)]) {
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onCdnPlayStatusUpdate, status:onLoading, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
        [self.delegate onCdnPlayStatusUpdate:CdnPlayStatusLoading];
    }
}

- (void)onAudioPlaying:(id<V2TXLivePlayer>)player firstPlay:(BOOL)firstPlay extraInfo:(NSDictionary *)extraInfo {
    NSLog(@"----- onAudioPlaying firstPlay: %d",firstPlay);
    if (self.delegate && [self.delegate respondsToSelector:@selector(onCdnPlayStatusUpdate:)]) {
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onCdnPlayStatusUpdate, status:onPlaying, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
        [self.delegate onCdnPlayStatusUpdate:CdnPlayStatusPlaying];
    }
}

- (void)onError:(id<V2TXLivePlayer>)player
           code:(V2TXLiveCode)code
        message:(NSString *)msg
      extraInfo:(NSDictionary *)extraInfo {
    if (code == V2TXLIVE_ERROR_DISCONNECTED) {
        if (self.delegate && [self.delegate respondsToSelector:@selector(onCdnPlayStatusUpdate:)]) {
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onCdnPlayStatusUpdate, status:stop, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
            [self.delegate onCdnPlayStatusUpdate:CdnPlayStatusStopped];
        }
    }
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager player onError, code:%ld, msg:%@, current_ntp:%ld", code, msg, [TXLiveBase getNetworkTimestamp]]];
}

- (void)onWarning:(id<V2TXLivePlayer>)player
             code:(V2TXLiveCode)code
          message:(NSString *)msg
        extraInfo:(NSDictionary *)extraInfo {
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager player onWarning, code:%ld, msg:%@, current_ntp:%ld", code, msg, [TXLiveBase getNetworkTimestamp]]];
}

#pragma mark - V2TXLivePusherObserver
- (void)onPushStatusUpdate:(V2TXLivePushStatus)status message:(NSString *)msg extraInfo:(NSDictionary *)extraInfo {
    [self asyncDelegate:^{
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onCdnPushStatusUpdate, v2_status:%ld, current_ntp:%ld", status, [TXLiveBase getNetworkTimestamp]]];
        if (self.delegate && [self.delegate respondsToSelector:@selector(onCdnPushStatusUpdate:)]) {
            CdnPushStatus result;
            switch (status) {
                case V2TXLivePushStatusConnecting:
                    result = CdnPushStatusConnecting;
                    break;
                case V2TXLivePushStatusDisconnected:
                    result = CdnPushStatusDisconnected;
                    break;
                case V2TXLivePushStatusReconnecting:
                    result = CdnPushStatusReconnecting;
                    break;
                case V2TXLivePushStatusConnectSuccess:
                    result = CdnPushStatusConnectSuccess;
                    break;
                default:
                    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onCdnPushStatusUpdate, v2_status translate error, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
                    break;
            }
            [self.delegate onCdnPushStatusUpdate:result];
        }
    }];
}

- (void)onError:(V2TXLiveCode)code message:(NSString *)msg extraInfo:(NSDictionary *)extraInfo {
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager pusher onError, code:%ld, msg:%@, current_ntp:%ld", code, msg, [TXLiveBase getNetworkTimestamp]]];
}

- (void)onWarning:(V2TXLiveCode)code message:(NSString *)msg extraInfo:(NSDictionary *)extraInfo {
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager pusher onWarning, code:%ld, msg:%@, current_ntp:%ld", code, msg, [TXLiveBase getNetworkTimestamp]]];
}

#pragma mark - Private Methods
#pragma mark - cdn相关
- (void)initPusher {
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager initPusher, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
    self.pusher = [[V2TXLivePusher alloc] initWithLiveMode:V2TXLIVEMODE_SIMPLE];
    [self.pusher setObserver:self];
    V2TXLiveVideoEncoderParam *param = [V2TXLiveVideoEncoderParam new];
    param.videoResolution = V2TXLiveVideoResolution960x540;
    param.videoResolutionMode = V2TXLiveVideoResolutionModePortrait;
    param.minVideoBitrate = 800;
    param.videoBitrate = 1500;
    param.videoFps = 15;
    [self.pusher setVideoQuality:param];
    [self.pusher setAudioQuality:V2TXLiveAudioQualityDefault];
}

- (void)initPlayer {
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager initPlayer, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
    self.player = [[V2TXLivePlayer alloc] init];
    [self.player setObserver:self];
    [self.player enableReceiveSeiMessage:YES payloadType:CHORUS_SEI_PAYLOAD_TYPE];
}

- (BOOL)isCdnPushing {
    return self.pusher.isPushing;
}

- (BOOL)isCdnPlaying {
    return self.player.isPlaying;
}

#pragma mark - 停止合唱相关
/// 停止主播端合唱播放
- (void)stopLocalChorus {
    int32_t musicID = self.currentPlayMusicID;
    //合唱中，清理状态
    self.requestStopChorusTs = [TXLiveBase getNetworkTimestamp];
    [self sendStopChorusMsg];
    TRTCLog(@"___ stop %d", musicID);
    [[self audioEffecManager] stopPlayMusic:musicID];
    [self clearChorusState];
    _isChorusOn = NO;
    self.currentPlayMusicID = -1;
    self.startPlayChorusMusicTs = -1;
    self.requestStopChorusTs = -1;
    self.startDelayMs = -1;
    [self asyncDelegate:^{
        if (self.delegate && [self.delegate respondsToSelector:@selector(onChorusStop:message:)]) {
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onChorusStop, reason:ChorusStopReasonLocal, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
            [self.delegate onChorusStop:ChorusStopReasonLocal message:@"local user stopped chorus"];
        }
        if (self.delegate && [self.delegate respondsToSelector:@selector(onMusicCompletePlaying:)]) {
            [self.delegate onMusicCompletePlaying:musicID];
        }
    }];
}

/// 停止副唱端合唱播放
- (void)stopRemoteChorus {
    int32_t musicID = self.currentPlayMusicID;
    //合唱中，清理状态
    self.requestStopChorusTs = [TXLiveBase getNetworkTimestamp];
    TRTCLog(@"___ stop %d", musicID);
    [[self audioEffecManager] stopPlayMusic:musicID];
    [self clearChorusState];
    _isChorusOn = NO;
    self.currentPlayMusicID = -1;
    self.startPlayChorusMusicTs = -1;
    self.requestStopChorusTs = -1;
    self.startDelayMs = -1;
    [self asyncDelegate:^{
        if (self.delegate && [self.delegate respondsToSelector:@selector(onChorusStop:message:)]) {
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onChorusStop, reason:ChorusStopReasonRemote, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
            [self.delegate onChorusStop:ChorusStopReasonRemote message:@"remote user stopped chorus"];
        }
    }];
}

/// 合唱结束清理合唱状态
- (void)clearChorusState {
    if (_delayStartChorusMusicTimer) {
        dispatch_source_cancel(_delayStartChorusMusicTimer);
        _delayStartChorusMusicTimer = nil;
    }
    if (self.preloadMusicTimer) {
        dispatch_source_cancel(self.preloadMusicTimer);
        self.preloadMusicTimer = nil;
    }
    [self.chorusLongTermTimer setFireDate:[NSDate distantFuture]];
}


#pragma mark - 播放音乐方法
- (void)schedulePlayMusic:(NSInteger)delayMs {
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager schedulePlayMusic delayMs:%ld, current_ntp:%ld", delayMs, [TXLiveBase getNetworkTimestamp]]];
    CHORUS_WEAKIFY(self);
    TXAudioMusicStartBlock startBlock = ^(NSInteger errCode) {
        CHORUS_STRONGIFY_OR_RETURN(self);
        if (errCode == 0) {
            [self asyncDelegate:^{
                CHORUS_STRONGIFY_OR_RETURN(self);
                if ([self canDelegateResponseMethod:@selector(onMusicPrepareToPlay:)]) {
                    [self.delegate onMusicPrepareToPlay:self.currentPlayMusicID];
                }
            }];
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:
            @"TRTCChorusManager start play music, current_progress:%ld, current_ntp:%ld", [[self
            audioEffecManager] getMusicCurrentPosInMS:self.currentPlayMusicID], [TXLiveBase getNetworkTimestamp]]];
        } else {
            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager start play music failed %ld, current_ntp:%ld", errCode, [TXLiveBase getNetworkTimestamp]]];
            [self clearChorusState];
            self->_isChorusOn = NO;
            self.currentPlayMusicID = -1;
            self.startPlayChorusMusicTs = -1;
            self.startDelayMs = -1;
            [self asyncDelegate:^{
                if (self.delegate && [self.delegate respondsToSelector:@selector(onChorusStop:message:)]) {
                    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onChorusStop, reason:ChorusStopReasonMusicFailed, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
                    [self.delegate onChorusStop:ChorusStopReasonMusicFailed message:@"music start failed"];
                }
            }];
        }
    };
    
    TXAudioMusicProgressBlock progressBlock = ^(NSInteger progressMs, NSInteger durationMs) {
        CHORUS_STRONGIFY_OR_RETURN(self);
        //通知歌曲进度，用户会在这里进行歌词的滚动
        [self asyncDelegate:^{
            if (self.delegate && [self.delegate respondsToSelector:@selector(onMusicProgressUpdate:progress:duration:)]) {
                [self.delegate onMusicProgressUpdate:self.currentPlayMusicID progress:progressMs duration:durationMs];
            }
        }];
        if (self.pusher.isPushing) {
            NSDictionary *progressMsg = @{
                KMusicCurrentTs: @([[self audioEffecManager] getMusicCurrentPosInMS:self.currentPlayMusicID]),
                KMusicCurrentId: @(self.currentPlayMusicID),
            };
            NSString *jsonString = [self jsonStringFrom:progressMsg];
            [self.pusher sendSeiMessage:CHORUS_SEI_PAYLOAD_TYPE data:[jsonString dataUsingEncoding:NSUTF8StringEncoding]];
        }
    };
    
    TXAudioMusicCompleteBlock completedBlock = ^(NSInteger errCode){
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager music play completed, errCode:%ld current_ntp:%ld", errCode, [TXLiveBase getNetworkTimestamp]]];
        TRTCLog(@"___ chorus: on complete: %ld", errCode);
        CHORUS_STRONGIFY_OR_RETURN(self);
        //播放完成后停止自定义消息的发送
        [self clearChorusState];
        //通知合唱已结束
        self->_isChorusOn = NO;
        self.startPlayChorusMusicTs = -1;
        self.startDelayMs = -1;
        [self asyncDelegate:^{
            if ([self canDelegateResponseMethod:@selector(onMusicCompletePlaying:)]) {
                [self.delegate onMusicCompletePlaying:self.currentPlayMusicID];
            }
            if ([self canDelegateResponseMethod:@selector(onChorusStop:message:)]) {
                [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager calling onChorusStop, reason:ChorusStopReasonMusicFinished, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
                [self.delegate onChorusStop:ChorusStopReasonMusicFinished message:@"chorus music finished playing"];
            }
            self.currentPlayMusicID = -1;
        }];
    };
    
    if (delayMs > 0) {
        [self preloadMusic:self.musicParam.path startMs:0];
        if (!self.delayStartChorusMusicTimer) {
            NSInteger initialTime = [TXLiveBase getNetworkTimestamp];
            self.delayStartChorusMusicTimer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0,
             dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0));
            dispatch_source_set_timer(self.delayStartChorusMusicTimer, DISPATCH_TIME_NOW, DISPATCH_TIME_FOREVER, 0);
            dispatch_source_set_event_handler(self.delayStartChorusMusicTimer, ^{
                while (true) {
                    //轮询，直到当前时间为约定好的播放时间再进行播放，之所以不直接用timer在约定时间执行是由于精度问题，可能会相差几百毫秒
                    CHORUS_STRONGIFY_OR_RETURN(self);
                    if ([TXLiveBase getNetworkTimestamp] > (initialTime + delayMs)) {
                        if(!self->_isChorusOn) {
                            //若达到预期播放时间时，合唱已被停止，则跳过此次播放
                            [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager schedulePlayMusic abort, chorus has been stopped, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
                            break;
                        }
                        [[self audioEffecManager] startPlayMusic:self.musicParam onStart:startBlock
                         onProgress:progressBlock onComplete:completedBlock];
                        break;
                    }
                }
            });
            dispatch_resume(_delayStartChorusMusicTimer);
        }
    } else {
        [[self audioEffecManager] startPlayMusic:self.musicParam onStart:startBlock onProgress:progressBlock
         onComplete:completedBlock];
        if (delayMs < 0) {
            NSInteger startMS = -delayMs + CHORUS_PRELOAD_MUSIC_DELAY;
            [self preloadMusic:self.musicParam.path startMs:startMS];
            if (!self.preloadMusicTimer) {
                NSInteger initialTime = [TXLiveBase getNetworkTimestamp];
                self.preloadMusicTimer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0,
                 dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0));
                dispatch_source_set_timer(self.preloadMusicTimer, DISPATCH_TIME_NOW, DISPATCH_TIME_FOREVER, 0);
                dispatch_source_set_event_handler(self.preloadMusicTimer, ^{
                    while (true) {
                        //轮询，直到当前时间为约定时间再执行，之所以不直接用timer在约定时间执行是由于精度问题，可能会相差几百毫秒
                        CHORUS_STRONGIFY_OR_RETURN(self);
                        if ([TXLiveBase getNetworkTimestamp] > (initialTime + CHORUS_PRELOAD_MUSIC_DELAY)) {
                            if(!self->_isChorusOn) {
                                //若达到预期播放时间时，合唱已被停止，则跳过此次播放
                                TRTCLog(@"%@",[NSString stringWithFormat:@"TRTCChorusManager schedulePlayMusic abort, chorus has been stopped, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]);
                                break;
                            }
                            [[self audioEffecManager] startPlayMusic:self.musicParam onStart:startBlock
                             onProgress:progressBlock onComplete:completedBlock];
                            TRTCLog(@"%@",[NSString stringWithFormat:@"TRTCChorusManager calling startPlayMusic, startMs:%ld, current_ntp:%ld", startMS, [TXLiveBase getNetworkTimestamp]]);
                            break;
                        }
                    }
                });
                dispatch_resume(self.preloadMusicTimer);
            }
        }
    }
}

#pragma mark - 歌曲同步方法
- (void)preloadMusic:(NSString *)path startMs:(NSInteger)startMs {
    [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager preloadMusic, current_ntp:%ld", [TXLiveBase getNetworkTimestamp]]];
    NSDictionary *jsonDict = @{
        @"api": @"preloadMusic",
        @"params": @{
                @"musicId": @(self.currentPlayMusicID),
                @"path": path,
                @"startTimeMS": @(startMs),
        }
    };
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:jsonDict options:0 error:NULL];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    [[TRTCCloud sharedInstance] callExperimentalAPI:jsonString];
}

#pragma mark - 发送合唱信令相关
- (void)sendStartChorusMsg {
    NSDictionary *json = @{
        KChorusCmd: KChorusCmdStart,
        KChorusTimestampPlay: @(self.startPlayChorusMusicTs),
        KChorusMusicID: [NSString stringWithFormat:@"%d",self.currentPlayMusicID],
        KChorusMusicDuration: [NSString stringWithFormat:@"%ld",self.musicDuration],
    };
    NSString *jsonString = [self jsonStringFrom:json];
    [self sendCustomMessage:jsonString reliable:NO];
}

- (void)sendStopChorusMsg {
    NSDictionary *json = @{
        KChorusCmd: KChorusCmdStop,
        KChorusTimestampStop: @(self.requestStopChorusTs),
        KChorusMusicID: [NSString stringWithFormat:@"%d",self.currentPlayMusicID],
    };
    NSString *jsonString = [self jsonStringFrom:json];
    [self sendCustomMessage:jsonString reliable:YES];
}

- (NSString *)jsonStringFrom:(NSDictionary *)dict {
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dict options:0 error:NULL];
    return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
}

- (BOOL)sendCustomMessage:(NSString *)message reliable:(BOOL)reliable {
    NSData * _Nullable data = [message dataUsingEncoding:NSUTF8StringEncoding];
    if (data != nil) {
        return [[TRTCCloud sharedInstance] sendCustomCmdMsg:0 data:data reliable:reliable ordered:reliable];
    }
    return NO;
}

- (void)checkMusicProgress {
    NSInteger currentProgress = [[self audioEffecManager] getMusicCurrentPosInMS:self.currentPlayMusicID];
    NSInteger estimatedProgress = [TXLiveBase getNetworkTimestamp] - self.startPlayChorusMusicTs;
    if (estimatedProgress >= 0 && labs(currentProgress - estimatedProgress) > 60) {
        [[TRTCCloud sharedInstance] apiLog:[NSString stringWithFormat:@"TRTCChorusManager checkMusicProgress triggered seek, currentProgress:%ld, estimatedProgress:%ld, current_ntp:%ld", currentProgress, estimatedProgress, [TXLiveBase getNetworkTimestamp]]];
        [[self audioEffecManager] seekMusicToPosInMS:self.currentPlayMusicID pts:estimatedProgress];
    }
}

#pragma mark - NTP校准
- (BOOL)isNtpReady {
    return [TXLiveBase getNetworkTimestamp] > 0;
}

- (TXAudioEffectManager *)audioEffecManager {
    return [[TRTCCloud sharedInstance] getAudioEffectManager];
}

- (BOOL)canDelegateResponseMethod:(SEL)method {
    return self.delegate && [self.delegate respondsToSelector:method];
}

- (void)runMainQueue:(void(^)(void))action {
    CHORUS_WEAKIFY(self);
    dispatch_async(dispatch_get_main_queue(), ^{
        CHORUS_STRONGIFY_OR_RETURN(self);
        action();
    });
}

- (void)asyncDelegate:(void(^)(void))block {
    CHORUS_WEAKIFY(self);
    dispatch_async(dispatch_get_main_queue(), ^{
        CHORUS_STRONGIFY_OR_RETURN(self);
        block();
    });
}
@end
