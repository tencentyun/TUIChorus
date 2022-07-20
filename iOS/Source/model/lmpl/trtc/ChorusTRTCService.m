//
//  ChorusTRTCService.m
//  TUIChorus
//
//  Created by adams on 2021/7/15.
//  Copyright © 2022 Tencent. All rights reserved.

#import "ChorusTRTCService.h"
#import "TRTCCloud.h"

static const int TC_COMPONENT_CHORUS = 9;
static const int TC_TRTC_FRAMEWORK   = 1;

@interface ChorusTRTCService() <TRTCCloudDelegate>
@property (nonatomic, assign) BOOL isInRoom;
@property (nonatomic, strong) NSString *userId;
@property (nonatomic, strong) NSString *roomId;
@property (nonatomic, strong) TRTCParams *mTRTCParms;
@property (nonatomic, copy) TXChorusCallback enterRoomCallback;
@property (nonatomic, copy) TXChorusCallback exitRoomCallback;
@property (nonatomic, strong, readonly)TRTCCloud *mTRTCCloud;
@end

@implementation ChorusTRTCService

- (TRTCCloud *)mTRTCCloud {
    return [TRTCCloud sharedInstance];
}

#pragma mark - public method
+ (instancetype)sharedInstance {
    static ChorusTRTCService* instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[ChorusTRTCService alloc] init];
    });
    return instance;
}

- (void)exitRoom:(TXChorusCallback _Nullable)callback {
    TRTCLog(@"exit trtc room.");
    self.userId = nil;
    self.mTRTCParms = nil;
    self.enterRoomCallback = nil;
    self.exitRoomCallback = callback;
    [self.mTRTCCloud exitRoom];
}

- (void)enterRoomWithSdkAppId:(UInt32)sdkAppId roomId:(NSString *)roomId userId:(NSString *)userId userSign:(NSString *)userSign role:(NSInteger)role callback:(TXChorusCallback _Nullable)callback {
    BOOL isParamError = NO;
    if (roomId == nil || [roomId isEqualToString:@""]) {
        isParamError = YES;
    }
    if (userId == nil || [userId isEqualToString:@""]) {
        isParamError = YES;
    }
    if (userSign == nil || [userSign isEqualToString:@""]) {
        isParamError = YES;
    }
    int roomIdIntValue = [roomId intValue];
    if (roomIdIntValue == 0) {
        isParamError = YES;
    }
    if (isParamError) {
        TRTCLog(@"error: enter trtc room fail. params invalid. room id:%@, userId:%@, userSig is empty:%d", roomId, userId, (userSign == nil || [userSign isEqualToString:@""]));
        callback(-1, @"enter trtc room fail.");
        return;
    }
    self.userId = userId;
    self.roomId = roomId;
    self.enterRoomCallback = callback;
    TRTCLog(@"enter room. app id:%u, room id: %@, userID: %@", (unsigned int)sdkAppId, roomId, userId);
    TRTCParams * parms = [[TRTCParams alloc] init];
    parms.sdkAppId = sdkAppId;
    parms.userId = userId;
    parms.userSig = userSign;
    parms.role = role == 20 ? TRTCRoleAnchor : TRTCRoleAudience;
    parms.roomId = roomIdIntValue;
    self.mTRTCParms = parms;
    [self internalEnterRoom];
}

- (void)setAudioQuality:(NSInteger)quality {
    TRTCAudioQuality targetQuality = TRTCAudioQualityDefault;
    switch (quality) {
        case 1:
            targetQuality = TRTCAudioQualitySpeech;
            break;
        case 3:
            targetQuality = TRTCAudioQualityMusic;
        default:
            break;
    }
    [self.mTRTCCloud setAudioQuality:targetQuality];
}

- (void)muteAllRemoteAudio:(BOOL)isMute {
    [self.mTRTCCloud muteAllRemoteAudio:isMute];
}

- (void)enableRealtimeChorus:(BOOL)enable {
    NSDictionary *json = @{
        @"api" : @"enableRealtimeChorus",
        @"params" : @{
                @"enable" : @(enable)
        }
    };
    NSError *err = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:json options:NSJSONWritingPrettyPrinted error:&err];
    if (!err) {
        NSString *jsonStr = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        [self.mTRTCCloud callExperimentalAPI:jsonStr];
    }
}

- (void)sendSEIMsg:(NSData *)data {
    BOOL res = [self.mTRTCCloud sendSEIMsg:data repeatCount:1];
    if (!res) {
        TRTCLog(@"___ send SEI failed");
    }
}

- (BOOL)sendCustomMessage:(NSString *)message reliable:(BOOL)reliable {
    NSData * _Nullable data = [message dataUsingEncoding:NSUTF8StringEncoding];
    if (data != nil) {
        return [self.mTRTCCloud sendCustomCmdMsg:0 data:data reliable:reliable ordered:reliable];
    }
    return NO;
}

- (void)switchToAnchor {
    [self.mTRTCCloud switchRole:TRTCRoleAnchor];
    [self.mTRTCCloud startLocalAudio];
}

- (void)switchToAudience {
    [self.mTRTCCloud stopLocalAudio];
    [self.mTRTCCloud switchRole:TRTCRoleAudience];
}

- (void)muteLocalAudio:(BOOL)isMute {
    [self.mTRTCCloud muteLocalAudio:isMute];
    TRTCLog(@"mute local %d", isMute);
}

- (void)setVoiceEarMonitorEnable:(BOOL)enable {
    [[self.mTRTCCloud getAudioEffectManager] enableVoiceEarMonitor:enable];
    TRTCLog(@"ear monitor %@", enable ? @"enable" : @"disable");
}

- (void)startMicrophone {
    [self.mTRTCCloud startLocalAudio];
}

- (void)stopMicrophone {
    [self.mTRTCCloud stopLocalAudio];
}

- (void)setAudioCaptureVolume:(NSInteger)volume {
    [self.mTRTCCloud setAudioCaptureVolume:volume];
}

- (void)setAudioPlayoutVolume:(NSInteger)volume {
    [self.mTRTCCloud setAudioPlayoutVolume:volume];
}

- (void)setSpeaker:(BOOL)userSpeaker {
    [self.mTRTCCloud setAudioRoute:userSpeaker ? TRTCAudioModeSpeakerphone : TRTCAudioModeEarpiece];
}

#pragma mark - private method
- (void)internalEnterRoom {
    if (self.mTRTCParms) {
        self.mTRTCCloud.delegate = self;
        [self enableAudioEvalutation:YES];
        [self setFramework];
        [self.mTRTCCloud enterRoom:self.mTRTCParms appScene:TRTCAppSceneVoiceChatRoom];
    }
}

- (void)setFramework {
    NSDictionary *jsonDic = @{@"api": @"setFramework",
                              @"params":@{@"framework": @(TC_TRTC_FRAMEWORK),
                                          @"component": @(TC_COMPONENT_CHORUS)}};
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:jsonDic options:NSJSONWritingPrettyPrinted error:nil];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    TRTCLog(@"jsonString = %@",jsonString);
    [self.mTRTCCloud callExperimentalAPI: jsonString];
}

- (void)enableAudioEvalutation:(BOOL)enable {
    [self.mTRTCCloud enableAudioVolumeEvaluation:enable ? 300 : 0];
}

- (BOOL)canDelegateResponseMethod:(SEL)method {
    return self.delegate && [self.delegate respondsToSelector:method];
}

#pragma mark - TRTCCloudDelegate
- (void)onEnterRoom:(NSInteger)result {
    TRTCLog(@"on enter trtc room. result:%ld", (long)result);
    if (result > 0) {
        self.isInRoom = YES;
        if (self.enterRoomCallback) {
            self.enterRoomCallback(0, @"enter trtc room success.");
        }
    } else {
        self.isInRoom = NO;
        NSString *errorMsg = (result == ERR_TRTC_USER_SIG_CHECK_FAILED ? @"userSig invalid, please login again.":@"enter trtc room fail.");
        if (self.enterRoomCallback) {
            self.enterRoomCallback((int)result, errorMsg);
        }
    }
    self.enterRoomCallback = nil;
}

- (void)onExitRoom:(NSInteger)reason {
    TRTCLog(@"on exit trtc room. reslut: %ld", (long)reason);
    self.isInRoom = NO;
    if (self.exitRoomCallback) {
        self.exitRoomCallback(0, @"exite room success");
    }
    self.exitRoomCallback = nil;
}

- (void)onRemoteUserEnterRoom:(NSString *)userId {
    TRTCLog(@"on user enter, userid: %@", userId);
    if ([self canDelegateResponseMethod:@selector(onTRTCAnchorEnter:)]) {
        [self.delegate onTRTCAnchorEnter:userId];
    }
}

- (void)onRemoteUserLeaveRoom:(NSString *)userId reason:(NSInteger)reason {
    if ([self canDelegateResponseMethod:@selector(onTRTCAnchorExit:)]) {
        [self.delegate onTRTCAnchorExit:userId];
    }
}

- (void)onUserAudioAvailable:(NSString *)userId available:(BOOL)available {
    if ([self canDelegateResponseMethod:@selector(onTRTCAudioAvailable:available:)]) {
        [self.delegate onTRTCAudioAvailable:userId available:available];
    }
}

- (void)onError:(TXLiteAVError)errCode errMsg:(NSString *)errMsg extInfo:(NSDictionary *)extInfo{
    if ([self canDelegateResponseMethod:@selector(onError:message:)]) {
        [self.delegate onError:errCode message:errMsg];
    }
}

- (void)onNetworkQuality:(TRTCQualityInfo *)localQuality remoteQuality:(NSArray<TRTCQualityInfo *> *)remoteQuality {
    if ([self canDelegateResponseMethod:@selector(onNetWorkQuality:arrayList:)]) {
        [self.delegate onNetWorkQuality:localQuality arrayList:remoteQuality];
    }
}

- (void)onUserVoiceVolume:(NSArray<TRTCVolumeInfo *> *)userVolumes totalVolume:(NSInteger)totalVolume {
    if ([self canDelegateResponseMethod:@selector(onUserVoiceVolume:totalVolume:)]) {
        [self.delegate onUserVoiceVolume:userVolumes totalVolume:totalVolume];
    }
}

- (void)onRecvSEIMsg:(NSString *)userId message:(NSData *)message {
    if ([self canDelegateResponseMethod:@selector(onRecvSEIMsg:message:)]) {
        [self.delegate onRecvSEIMsg:userId message:message];
    }
}

- (void)onRecvCustomCmdMsgUserId:(NSString *)userId cmdID:(NSInteger)cmdID seq:(UInt32)seq message:(NSData *)message {
    if ([self canDelegateResponseMethod:@selector(onRecvCustomCmdMsgUserId:cmdID:seq:message:)]) {
        [self.delegate onRecvCustomCmdMsgUserId:userId cmdID:cmdID seq:seq message:message];
    }
}

@end
