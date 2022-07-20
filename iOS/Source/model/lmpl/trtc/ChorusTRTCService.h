//
//  ChorusTRTCService.h
//  TUIChorus
//
//  Created by adams on 2021/7/15.
//  Copyright © 2022 Tencent. All rights reserved.

#import <Foundation/Foundation.h>
#import "TXChorusBaseDef.h"

NS_ASSUME_NONNULL_BEGIN

#define kTRTCRoleAnchorValue 20
#define kTRTCRoleAudienceValue 21

@class TRTCQualityInfo;
@class TRTCVolumeInfo;

@protocol ChorusTRTCServiceDelegate <NSObject>
- (void)onTRTCAnchorEnter:(NSString *)userId;
- (void)onTRTCAnchorExit:(NSString *)userId;
- (void)onTRTCAudioAvailable:(NSString *)userId available:(BOOL)available;
- (void)onError:(NSInteger)code message:(NSString *)message;
- (void)onNetWorkQuality:(TRTCQualityInfo *)trtcQuality arrayList:(NSArray<TRTCQualityInfo *> *)arrayList;
- (void)onUserVoiceVolume:(NSArray<TRTCVolumeInfo *> *)userVolumes totalVolume:(NSInteger)totalVolume;
- (void)onRecvSEIMsg:(NSString *)userId message:(NSData *)message;
- (void)onRecvCustomCmdMsgUserId:(NSString *)userId cmdID:(NSInteger)cmdID seq:(UInt32)seq message:(NSData *)message;
@end

@interface ChorusTRTCService : NSObject

@property (nonatomic, weak) id<ChorusTRTCServiceDelegate> delegate;

+ (instancetype)sharedInstance;

- (void)exitRoom:(TXChorusCallback _Nullable)callback;

- (void)enterRoomWithSdkAppId:(UInt32)sdkAppId roomId:(NSString *)roomId userId:(NSString *)userId userSign:(NSString *)userSign role:(NSInteger)role callback:(TXChorusCallback _Nullable)callback;

- (void)setAudioQuality:(NSInteger)quality;

- (void)muteAllRemoteAudio:(BOOL)isMute;

- (void)sendSEIMsg:(NSData *)data;

- (BOOL)sendCustomMessage:(NSString *)message reliable:(BOOL)reliable;

- (void)enableRealtimeChorus:(BOOL)enable;

- (void)switchToAnchor;

- (void)switchToAudience;

- (void)muteLocalAudio:(BOOL)isMute;

- (void)setVoiceEarMonitorEnable:(BOOL)enable;

- (void)startMicrophone;

- (void)stopMicrophone;

- (void)setAudioCaptureVolume:(NSInteger)volume;

- (void)setAudioPlayoutVolume:(NSInteger)volume;

- (void)setSpeaker:(BOOL)userSpeaker;

@end

NS_ASSUME_NONNULL_END
