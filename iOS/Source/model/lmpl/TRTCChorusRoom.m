//
//  TRTCChorusRoom.m
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

#import "TRTCChorusRoom.h"
#import "TXChorusCommonDef.h"
#import "TRTCCloudDef.h"
#import "TXChorusBaseDef.h"
#import "TXChorusService.h"
#import "TRTCChorusRoomDelegate.h"
#import "ChorusTRTCService.h"
#import "TRTCCloud.h"
#import "TXLiveBase.h"
#import "TRTCChorusManager.h"
#import "ChorusLocalized.h"
#import <CommonCrypto/CommonCrypto.h>

typedef enum : NSUInteger {
    TRTCChorusRoleAnchor = 0,   //主唱（房主）
    TRTCChorusRoleChorus = 1,   //副唱
    TRTCChorusRoleAudience = 2, //观众
} TRTCChorusRole;

@interface TRTCChorusRoom()<ChorusTRTCServiceDelegate,TXChorusRoomServiceDelegate,TRTCChorusManagerDelegate>

@property (nonatomic, assign) int mSDKAppID;

@property (nonatomic, strong) NSString *userId;
@property (nonatomic, strong) NSString *userSig;
@property (nonatomic, strong) NSString *roomID;
@property (nonatomic, strong) NSString *rtmpPushURL;
@property (nonatomic, strong) NSString *rtmpPlayURL;
@property (nonatomic, strong) NSMutableSet<NSString *>         *anchorSeatList;
@property (nonatomic, strong) NSMutableArray<ChorusSeatInfo *> *seatInfoList;
@property (nonatomic, assign) NSInteger takeSeatIndex;

@property (nonatomic, weak) id<TRTCChorusRoomDelegate> delegate;

@property (nonatomic, copy, nullable) ActionCallback leaveSeatCallback;
@property (nonatomic, copy, nullable) ActionCallback enterSeatCallback;
@property (nonatomic, copy, nullable) ActionCallback pickSeatCallback;
@property (nonatomic, copy, nullable) ActionCallback kickSeatCallback;

@property (nonatomic, weak) dispatch_queue_t delegateQueue;

@property (nonatomic, readonly) TXChorusService     *roomService;
@property (nonatomic, readonly) ChorusTRTCService   *roomTRTCService;

@property (nonatomic, assign) BOOL isSelfMute;

@property (nonatomic, assign) int32_t currentPlayingMusicID;
@property (nonatomic, assign) TRTCChorusRole chorusRole;
@property (nonatomic, strong) TRTCChorusManager *chorusManager;

@end

@implementation TRTCChorusRoom

static TRTCChorusRoom *gInstance;
static dispatch_once_t gOnceToken;

- (instancetype)init {
    self = [super init];
    if (self) {
        self.delegateQueue = dispatch_get_main_queue();
        self.seatInfoList = [[NSMutableArray alloc] initWithCapacity:2];
        self.anchorSeatList = [[NSMutableSet alloc] initWithCapacity:2];
        self.takeSeatIndex = -1;
        self.roomService.delegate = self;
        self.roomTRTCService.delegate = self;
        self.isSelfMute = NO;
        self.currentPlayingMusicID = -1;
        self.chorusManager = [[TRTCChorusManager alloc] init];
        self.chorusManager.delegate = self;
    }
    return self;
}

- (TXChorusService *)roomService {
    return [TXChorusService sharedInstance];
}

- (ChorusTRTCService *)roomTRTCService {
    return [ChorusTRTCService sharedInstance];
}

- (TXAudioEffectManager *)getAudioEffectManager {
    return [[TRTCCloud sharedInstance] getAudioEffectManager];
}

- (BOOL)canDelegateResponseMethod:(SEL)method {
    return self.delegate && [self.delegate respondsToSelector:method];
}

- (void)setChorusRole:(TRTCChorusRole)chorusRole {
    _chorusRole = chorusRole;
    switch (chorusRole) {
        case TRTCChorusRoleAnchor:
            [self.chorusManager startCdnPush: self.rtmpPushURL];
            break;
        case TRTCChorusRoleAudience:
            [self.chorusManager startCdnPlay: self.rtmpPlayURL view:nil];
            break;
        case TRTCChorusRoleChorus:
            [self.chorusManager stopCdnPlay];
            break;
        default:
            break;
    }
}

#pragma mark - TRTCChorus 实现
+ (instancetype)sharedInstance {
    dispatch_once(&gOnceToken, ^{
        gInstance = [[TRTCChorusRoom alloc] init];
        [TXChorusService sharedInstance].delegate = gInstance;
        [ChorusTRTCService sharedInstance].delegate = gInstance;
    });
    return gInstance;
}

+ (void)destroySharedInstance {
    gOnceToken = 0;
    gInstance = nil;
}

- (void)setDelegateQueue:(dispatch_queue_t)queue {
    self->_delegateQueue = queue;
}

#pragma mark - 登录相关 实现
- (void)login:(int)sdkAppID userId:(NSString *)userId userSig:(NSString *)userSig callback:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if (sdkAppID != 0 && userId && ![userId isEqualToString:@""] && userSig && ![userSig isEqualToString:@""]) {
            self.mSDKAppID = sdkAppID;
            self.userId = userId;
            self.userSig = userSig;
            TRTCLog(@"start login room service");
            [self.roomService loginWithSdkAppId:sdkAppID userId:userId userSig:userSig callback:^(int code, NSString * _Nonnull message) {
                @strongify(self)
                if (!self) {
                    return;
                }
                [self.roomService getSelfInfo];
                if (callback) {
                    [self runOnDelegateQueue:^{
                        callback(code, message);
                    }];
                }
            }];
        } else {
            TRTCLog(@"start login failed. params invalid.");
            callback(-1, @"start login failed. params invalid.");
        }
    }];
}

- (void)logout:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        TRTCLog(@"start logout");
        self.mSDKAppID = 0;
        self.userId = @"";
        self.userSig = @"";
        TRTCLog(@"start logout room service");
        [self.roomService logout:^(int code, NSString * _Nonnull message) {
            if (callback) {
                [self runOnDelegateQueue:^{
                    callback(code, message);
                }];
            }
        }];
    }];
}

- (void)setSelfProfile:(NSString *)userName avatarURL:(NSString *)avatarURL callback:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomService setSelfProfileWithUserName:userName avatarUrl:avatarURL callback:^(int code, NSString * _Nonnull message) {
            if (callback) {
                [self runOnDelegateQueue:^{
                    callback(code, message);
                }];
            }
        }];
    }];
}

- (void)getAudienceList:(ChorusUserListCallback _Nullable)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomService getAudienceList:^(int code, NSString * _Nonnull message, NSArray<TXChorusUserInfo *> * _Nonnull userInfos) {
            TRTCLog(@"get audience list finish, code:%d, message:%@, userListCount:%ld", code, message, userInfos.count);
            NSMutableArray *userInfoList = [[NSMutableArray alloc] initWithCapacity:2];
            for (TXChorusUserInfo* info in userInfos) {
                ChorusUserInfo* userInfo = [[ChorusUserInfo alloc] init];
                userInfo.userId = info.userId;
                userInfo.userName = info.userName;
                userInfo.userAvatar = info.avatarURL;
                [userInfoList addObject:userInfo];
            }
            if (callback) {
                [self runOnDelegateQueue:^{
                    callback(code, message, userInfoList);
                }];
            }
        }];
    }];
}

- (void)enterTRTCRoomInnerWithRoomId:(NSString *)roomId userId:(NSString *)userId userSign:(NSString
 *)userSig role:(NSInteger)role callback:(ActionCallback)callback {
    TRTCLog(@"start enter trtc room.");
    @weakify(self)
    [self.roomTRTCService enableRealtimeChorus:YES];
    [self.roomTRTCService enterRoomWithSdkAppId:self.mSDKAppID roomId:roomId userId:userId
     userSign:userSig role:role callback:^(int code, NSString * _Nonnull message) {
        @strongify(self)
        if (!self) {
            return;
        }
        if (code == 0) {
            if (role == KTRTCRoleAnchorValue) {
                self.chorusRole = TRTCChorusRoleAnchor;
            }
            if (self.chorusRole == TRTCChorusRoleAudience) {
                self.chorusRole = TRTCChorusRoleChorus;
                ///观众上麦停止拉流
                [self.chorusManager stopCdnPlay];
            }
        }
        if (callback) {
            [self runOnDelegateQueue:^{
                callback(code, message);
            }];
        }
    }];
}

#pragma mark - 房间管理接口
- (void)createRoom:(int)roomID roomParam:(ChorusParam *)roomParam callback:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomService getSelfInfo];
        if (roomID == 0) {
            TRTCLog(@"crate room fail. params invalid.");
            if (callback) {
                callback(-1, @"create room fail. parms invalid.");
            }
            return;
        }
        self.roomID = [NSString stringWithFormat:@"%d", roomID];
        [self clearRoomStatus];
        self.rtmpPushURL = roomParam.rtmpPushURL;
        NSString* roomName = roomParam.roomName;
        NSString* roomCover = roomParam.coverUrl;
        BOOL isNeedrequest = roomParam.needRequest;
        NSInteger seatCount = roomParam.seatCount;
        NSMutableArray* seatInfoList = [[NSMutableArray alloc] initWithCapacity:2];
        if (roomParam.seatInfoList.count > 0) {
            for (ChorusSeatInfo* info in roomParam.seatInfoList) {
                TXChorusSeatInfo* seatInfo = [[TXChorusSeatInfo alloc] init];
                seatInfo.status = info.status;
                seatInfo.user = info.userId;
                [seatInfoList addObject:seatInfo];
                [self.seatInfoList addObject:info];
            }
        } else {
            for (int index = 0; index < seatCount; index += 1) {
                TXChorusSeatInfo* info = [[TXChorusSeatInfo alloc] init];
                [seatInfoList addObject:info];
                [self.seatInfoList addObject:[[ChorusSeatInfo alloc] init]];
            }
        }
        [self.roomService createRoomWithRoomId:self.roomID
                                      roomName:roomName
                                      coverUrl:roomCover
                                       playUrl:roomParam.rtmpPlayURL
                                   needRequest:isNeedrequest
                                  seatInfoList:seatInfoList
                                      callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            if (code == 0) {
                [self enterTRTCRoomInnerWithRoomId:self.roomID userId:self.userId userSign:self.userSig role:KTRTCRoleAnchorValue callback:callback];
                return;
            } else {
                [self runOnDelegateQueue:^{
                    if ([self canDelegateResponseMethod:@selector(onError:message:)]) {
                        [self.delegate onError:code message:message];
                    }
                }];
            }
            if (callback) {
                callback(code, message);
            }
        }];
    }];
}

- (void)destroyRoom:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        TRTCLog(@"start destroyu room.");
        if (self.chorusRole == TRTCChorusRoleAnchor || self.chorusRole == TRTCChorusRoleChorus) {
            [self.chorusManager stopCdnPush];
        } else if (self.chorusRole == TRTCChorusRoleAudience) {
            [self.chorusManager stopCdnPlay];
        }
        [self.roomTRTCService exitRoom:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            if (code != 0) {
                if ([self canDelegateResponseMethod:@selector(onError:message:)]) {
                    [self.delegate onError:code message:message];
                }
            }
        }];
        // 在公开群（Public）、会议（Meeting）和直播群（AVChatRoom）中，群主是不可以退群的，群主只能调用 dismissGroup 解散群组。
        [self.roomService destroyRoom:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            TRTCLog(@"destroy room finish, code:%d, message: %@", code, message);
            if (callback) {
                [self runOnDelegateQueue:^{
                    callback(code, message);
                }];
            }
        }];
        [self clearRoomStatus];
    }];
}

- (void)enterRoom:(NSInteger)roomID callback:(ActionCallback _Nullable)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self clearRoomStatus];
        self.roomID = [NSString stringWithFormat:@"%ld", (long)roomID];
        TRTCLog(@"start enter room, room id is %ld", (long)roomID);
        [self.roomService enterRoom:self.roomID callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            if (callback) {
                [self runMainQueue:^{
                    callback(code,message);
                }];
            }
            if (code != 0) {
                [self runOnDelegateQueue:^{
                    if ([self canDelegateResponseMethod:@selector(onError:message:)]) {
                        [self.delegate onError:code message:message];
                    }
                }];
            } else {
                self.rtmpPlayURL = self.roomService.roomInfo.playUrl;
                self.chorusRole = TRTCChorusRoleAudience;
            }
        }];
    }];
}

- (void)exitRoom:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        TRTCLog(@"start exit room");
        if ([self isOnSeatWithUserId:self.userId]) {
            [self leaveSeat:^(int code, NSString * _Nonnull message) {
                @strongify(self)
                if (!self) {
                    return;
                }
                [self exitRoomInternal:callback];
            }];
        } else {
            [self exitRoomInternal:callback];
        }
    }];
}

- (void)getRoomInfoList:(NSArray<NSNumber *> *)roomIdList callback:(ChorusRoomInfoCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        TRTCLog(@"start get room info:%@", roomIdList);
        NSMutableArray* roomIds = [[NSMutableArray alloc] initWithCapacity:2];
        for (NSNumber *roomId in roomIdList) {
            [roomIds addObject:[roomId stringValue]];
        }
        [self.roomService getRoomInfoList:roomIds calback:^(int code, NSString * _Nonnull message, NSArray<TXChorusRoomInfo *> * _Nonnull roomInfos) {
            if (code == 0) {
                TRTCLog(@"roomInfos: %@", roomInfos);
                NSMutableArray* trtcRoomInfos = [[NSMutableArray alloc] initWithCapacity:2];
                for (TXChorusRoomInfo *info in roomInfos) {
                    if ([info.roomId integerValue] != 0) {
                        ChorusRoomInfo *roomInfo = [[ChorusRoomInfo alloc] init];
                        roomInfo.roomID = [info.roomId integerValue];
                        roomInfo.ownerId = info.ownerId;
                        roomInfo.memberCount = info.memberCount;
                        roomInfo.roomName = info.roomName;
                        roomInfo.coverUrl = info.cover;
                        roomInfo.ownerName = info.ownerName;
                        roomInfo.needRequest = info.needRequest == 1;
                        [trtcRoomInfos addObject:roomInfo];
                    }
                }
                if (callback) {
                    callback(code, message, trtcRoomInfos);
                }
            } else {
                if (callback) {
                    callback(code, message, @[]);
                }
            }
        }];
    }];
}

- (void)getUserInfoList:(NSArray<NSString *> *)userIDList callback:(ChorusUserListCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if (!userIDList) {
            [self getAudienceList:callback];
            return;
        }
        [self.roomService getUserInfo:userIDList callback:^(int code, NSString * _Nonnull message, NSArray<TXChorusUserInfo *> * _Nonnull userInfos) {
            @strongify(self)
            if (!self) {
                return;
            }
            [self runOnDelegateQueue:^{
                @strongify(self)
                if (!self) {
                    return;
                }
                NSMutableArray* userList = [[NSMutableArray alloc] initWithCapacity:2];
                [userInfos enumerateObjectsUsingBlock:^(TXChorusUserInfo * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                    ChorusUserInfo* userInfo = [[ChorusUserInfo alloc] init];
                    userInfo.userId = obj.userId;
                    userInfo.userName = obj.userName;
                    userInfo.userAvatar = obj.avatarURL;
                    [userList addObject:userInfo];
                }];
                if (callback) {
                    callback(code, message, userList);
                }
            }];
        }];
    }];
}

- (void)enterSeat:(NSInteger)seatIndex callback:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self isOnSeatWithUserId:self.userId]) {
            [self runOnDelegateQueue:^{
                if (callback) {
                    callback(-1, @"you are alread in the seat.");
                }
            }];
            return;
        }
        self.enterSeatCallback = callback;
        [self.roomService takeSeat:seatIndex callback:^(int code, NSString * _Nonnull message) {
            if (code == 0) {
                TRTCLog(@"take seat callback success, and wait attrs changed");
            } else {
                self.enterSeatCallback = nil;
                self.takeSeatIndex = -1;
                if (callback) {
                    callback(code, message);
                }
            }
        }];
        
        if (self.chorusRole == TRTCChorusRoleAudience) {
            // 观众上麦需要进入TRTC房间
            [self enterTRTCRoomInnerWithRoomId:self.roomID userId:self.userId userSign:self.userSig
             role:KTRTCRoleAudienceValue callback:^(int code, NSString * _Nonnull message) {
                @strongify(self)
                if (!self) {
                    return;
                }
                if (callback) {
                    [self runMainQueue:^{
                        callback(code, message);
                    }];
                }
            }];
        }
    }];
}

- (void)leaveSeat:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        
        if (self.takeSeatIndex == -1) {
            [self runOnDelegateQueue:^{
                callback(-1, @"you are not in the seat.");
            }];
            return;
        }
        
        if (self.chorusRole == TRTCChorusRoleChorus) {
            if (self.chorusManager.isChorusOn) {
                [self runOnDelegateQueue:^{
                    callback(-1, chorusLocalize(@"Demo.TRTC.Chorus.cannotleavetheseat"));
                }];
                return;
            }
        }
        self.leaveSeatCallback = callback;
        [self.roomService leaveSeat:self.takeSeatIndex callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            if (code == 0) {
                TRTCLog(@"levae seat success. and wait attrs changed");
                if (self.chorusRole == TRTCChorusRoleChorus) {
                    [self.roomTRTCService exitRoom:^(int code, NSString * _Nonnull message) {
                        @strongify(self)
                        if (code == 0) {
                            self.chorusRole = TRTCChorusRoleAudience;
                            [self.chorusManager stopChorus];
                        }
                    }];
                }
            } else {
                self.leaveSeatCallback = nil;
                if (callback) {
                    callback(code, message);
                }
            }
        }];
    }];
}

- (void)pickSeat:(NSInteger)seatIndex userId:(NSString *)userId callback:(ActionCallback _Nullable)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self isOnSeatWithUserId:userId]) {
            [self runOnDelegateQueue:^{
                if (callback) {
                    callback(-1, @"");
                }
            }];
            return;
        }
        self.pickSeatCallback = callback;
        [self.roomService pickSeat:seatIndex userId:userId callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            if (code == 0) {
                TRTCLog(@"pick seat calback success. and wait attrs changed.");
            } else {
                self.pickSeatCallback = nil;
                if (callback) {
                    callback(code, message);
                }
            }
        }];
    }];
}

- (void)kickSeat:(NSInteger)seatIndex callback:(ActionCallback _Nullable)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        self.kickSeatCallback = callback;
        [self.roomService kickSeat:seatIndex callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            if (code == 0) {
                TRTCLog(@"kick seat calback success. and wait attrs changed.");
            } else {
                self.kickSeatCallback = nil;
                if (callback) {
                    callback(code, message);
                }
            }
        }];
    }];
}

- (void)closeSeat:(NSInteger)seatIndex isClose:(BOOL)isClose callback:(ActionCallback)callback{
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomService closeSeat:seatIndex isClose:isClose callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            [self runOnDelegateQueue:^{
                if (callback) {
                    callback(code, message);
                }
            }];
        }];
    }];
}

- (void)sendRoomTextMsg:(NSString *)message callback:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomService sendRoomTextMsg:message callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            [self runOnDelegateQueue:^{
                if (callback) {
                    callback(code, message);
                }
            }];
        }];
    }];
}

- (void)sendRoomCustomMsg:(NSString *)cmd message:(NSString *)message callback:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomService sendRoomCustomMsg:cmd message:message callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            [self runOnDelegateQueue:^{
                if (callback) {
                    callback(code, message);
                }
            }];
        }];
    }];
}

- (NSString *)sendInvitation:(NSString *)cmd userId:(NSString *)userId content:(NSString *)content callback:(ActionCallback)callback {
    @weakify(self)
    return [self.roomService sendInvitation:cmd userId:userId content:content callback:^(int code, NSString * _Nonnull message) {
        @strongify(self)
        if (!self) {
            return;
        }
        [self runOnDelegateQueue:^{
            if (callback) {
                callback(code, message);
            }
        }];
    }];
}

- (void)acceptInvitation:(NSString *)identifier callback:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomService acceptInvitation:identifier callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            [self runOnDelegateQueue:^{
                if (callback) {
                    callback(code, message);
                }
            }];
        }];
        
        // 观众上麦需要进入TRTC房间
        [self enterTRTCRoomInnerWithRoomId:self.roomID userId:self.userId userSign:self.userSig
         role:KTRTCRoleAudienceValue callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            if (callback) {
                [self runMainQueue:^{
                    callback(code, message);
                }];
            }
        }];
    }];
}

- (void)rejectInvitation:(NSString *)identifier callback:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomService rejectInvitaiton:identifier callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            [self runOnDelegateQueue:^{
                if (callback) {
                    callback(code, message);
                }
            }];
        }];
    }];
}

- (void)cancelInvitation:(NSString *)identifier callback:(ActionCallback)callback {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomService cancelInvitation:identifier callback:^(int code, NSString * _Nonnull message) {
            @strongify(self)
            if (!self) {
                return;
            }
            [self runOnDelegateQueue:^{
                if (callback) {
                    callback(code, message);
                }
            }];
        }];
    }];
}

#pragma mark - 本地音频操作接口
- (void)startMicrophone {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomTRTCService startMicrophone];
    }];
}

- (void)stopMicrophone {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomTRTCService stopMicrophone];
    }];
}

- (void)setAuidoQuality:(NSInteger)quality {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomTRTCService setAudioQuality:quality];
    }];
}

- (void)setVoiceEarMonitorEnable:(BOOL)enable {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomTRTCService setVoiceEarMonitorEnable:enable];
    }];
}

- (void)muteLocalAudio:(BOOL)mute {
    self.isSelfMute = mute;
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomTRTCService muteLocalAudio:mute];
    }];
}

- (void)setSpeaker:(BOOL)userSpeaker {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomTRTCService setSpeaker:userSpeaker];
    }];
}

- (void)setAudioCaptureVolume:(NSInteger)voluem {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomTRTCService setAudioCaptureVolume:voluem];
    }];
}

- (void)setAudioPlayoutVolume:(NSInteger)volume {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomTRTCService setAudioPlayoutVolume:volume];
    }];
}

#pragma mark - 远端用户接口
- (void)muteAllRemoteAudio:(BOOL)isMute {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self.roomTRTCService muteAllRemoteAudio:isMute];
    }];
}

- (void)startPlayMusic:(int32_t)musicID url:(NSString *)url {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (self.chorusRole == TRTCChorusRoleAnchor) {
            if (self.chorusManager.isChorusOn) {
                if (self.currentPlayingMusicID == musicID) {
                    [self resumePlayMusic];
                    return;
                }
                else {
                    [self stopPlayMusic];
                }
                self.currentPlayingMusicID = 0;
            }
        }
        self.currentPlayingMusicID = musicID;
        
        [self.chorusManager startChorus:[NSString stringWithFormat:@"%d", musicID] url:url
         reason:self.chorusRole == TRTCChorusRoleAnchor ? ChorusStartReasonLocal : ChorusStartReasonRemote];
    }];
}

- (void)stopPlayMusic {
    @weakify(self);
    [self runMainQueue:^{
        @strongify(self);
        [self.chorusManager stopChorus];
    }];
}

- (void)pausePlayMusic {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (self.currentPlayingMusicID != 0) {
            [[self getAudioEffectManager] pausePlayMusic:self.currentPlayingMusicID];
        }
    }];
}

- (void)resumePlayMusic {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (self.currentPlayingMusicID != 0) {
            [[self getAudioEffectManager] resumePlayMusic:self.currentPlayingMusicID];
        }
    }];
}

#pragma mark - private method
- (BOOL)isOnSeatWithUserId:(NSString *)userId {
    if (self.seatInfoList.count == 0) {
        return NO;
    }
    for (ChorusSeatInfo *seatInfo in self.seatInfoList) {
        if ([seatInfo.userId isEqualToString:userId]) {
            return YES;
        }
    }
    return NO;
}

- (void)runMainQueue:(void(^)(void))action {
    dispatch_async(dispatch_get_main_queue(), ^{
        action();
    });
}

- (void)runOnDelegateQueue:(void(^)(void))action {
    if (self.delegateQueue) {
        dispatch_async(self.delegateQueue, ^{
            action();
        });
    }
}

- (void)clearRoomStatus {
    [self.seatInfoList removeAllObjects];
    [self.anchorSeatList removeAllObjects];
    self.isSelfMute = NO;
    [self stopPlayMusic];
}

- (void)exitRoomInternal:(ActionCallback _Nullable)callback {
    @weakify(self)
    [self.roomTRTCService exitRoom:^(int code, NSString * _Nonnull message) {
        @strongify(self)
        if (!self) {
            return;
        }
        if (code != 0) {
            [self runOnDelegateQueue:^{
                if ([self canDelegateResponseMethod:@selector(onError:message:)]) {
                    [self.delegate onError:code message:message];
                }
            }];
        }
    }];
    TRTCLog(@"start exit room service");
    [self.roomService exitRoom:^(int code, NSString * _Nonnull message) {
        @strongify(self)
        if (!self) {
            return;
        }
        if (callback) {
            [self runOnDelegateQueue:^{
                callback(code, message);
            }];
        }
    }];
    [self clearRoomStatus];
    [self.roomTRTCService enableRealtimeChorus:NO];
    if (self.chorusRole == TRTCChorusRoleAudience) {
        if (self.chorusManager.isCdnPlaying) {
            [self.chorusManager stopCdnPlay];
        }
    } else if (self.chorusRole == TRTCChorusRoleAnchor) {
        if (self.chorusManager.isCdnPushing) {
            [self.chorusManager stopCdnPush];
        }
    }
    self.roomID = @"";
    _chorusRole = TRTCChorusRoleAnchor;
}

- (void)sendSEIMsg:(NSDictionary *)json {
    NSError *err = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:json options:NSJSONWritingPrettyPrinted error:&err];
    if (err == nil) {
        [self.roomTRTCService sendSEIMsg:data];
    }
}

#pragma mark - TRTCChorusManagerDelegate
- (void)onChorusStart:(ChorusStartReason)reason message:(NSString *)msg {
    TRTCLog(@"onChorusStart reason = %ld message = %@",reason, msg);
}

- (void)onChorusStop:(ChorusStopReason)reason message:(NSString *)msg {
    TRTCLog(@"onChorusStop reason = %ld message = %@",reason, msg);
    if (reason == ChorusStopReasonRemote) {
        
    }
}

- (void)onMusicPrepareToPlay:(int32_t)musicID {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onMusicPrepareToPlay:)]) {
            [self.delegate onMusicPrepareToPlay:musicID];
        }
    }];
}

- (void)onMusicCompletePlaying:(int32_t)musicID {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onMusicCompletePlaying:)]) {
            [self.delegate onMusicCompletePlaying:musicID];
        }
    }];
}

- (void)onMusicProgressUpdate:(int32_t)musicID progress:(NSInteger)progress duration:(NSInteger)durationMS {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onMusicProgressUpdate:progress:total:)]) {
            [self.delegate onMusicProgressUpdate:musicID progress:progress total:durationMS];
        }
    }];
}

- (void)onCdnPushStatusUpdate:(CdnPushStatus)status {
    TRTCLog(@"onCdnPushStatusUpdate = %ld",(long)status);
}

- (void)onCdnPlayStatusUpdate:(CdnPlayStatus)status {
    TRTCLog(@"onCdnPlayStatusUpdate = %ld",status);
}

- (void)onReceiveAnchorSendChorusMsg:(NSString *)musicID startDelay:(NSInteger)startDelay {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onReceiveAnchorSendChorusMsg:startDelay:)]) {
            [self.delegate onReceiveAnchorSendChorusMsg:musicID startDelay:startDelay];
        }
    }];
}

#pragma mark - ChorusTRTCServiceDelegate
- (void)onError:(NSInteger)code message:(nonnull NSString *)message {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onError:message:)]) {
            [self.delegate onError:(int)code message:message];
        }
    }];
}

- (void)onNetWorkQuality:(nonnull TRTCQualityInfo *)trtcQuality arrayList:(nonnull NSArray<TRTCQualityInfo *> *)arrayList {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onNetworkQuality:remoteQuality:)]) {
            [self.delegate onNetworkQuality:trtcQuality remoteQuality:arrayList];
        }
    }];
}

- (void)onRecvSEIMsg:(nonnull NSString *)userId message:(nonnull NSData *)message {
    @weakify(self)
    NSError *err = nil;
    NSDictionary *dic = [NSJSONSerialization JSONObjectWithData:message options:NSJSONReadingMutableContainers error:&err];
    if (err || ![dic isKindOfClass:[NSDictionary class]]) {
        TRTCLog(@"___ recv SEI class failed");
        return;
    }
    if ([dic.allKeys containsObject:@"music_id"] && [dic.allKeys containsObject:@"current_time"] && [dic.allKeys containsObject:@"total_time"]) {
        [self runOnDelegateQueue:^{
            @strongify(self)
            if ([self canDelegateResponseMethod:@selector(onMusicProgressUpdate:progress:total:)]) {
                [self.delegate onMusicProgressUpdate:[dic[@"music_id"] intValue] progress:[dic[@"current_time"] doubleValue] total:[dic[@"total_time"] doubleValue]];
            }
        }];
    }
}

- (void)onTRTCAnchorEnter:(nonnull NSString *)userId {
    [self.anchorSeatList addObject:userId];
}

- (void)onTRTCAnchorExit:(nonnull NSString *)userId {
    if (self.roomService.isOwner) {
        if (self.seatInfoList.count > 0) {
            NSInteger kickSeatIndex = -1;
            for (int i = 0; i<self.seatInfoList.count; i+=1) {
                if ([userId isEqualToString:self.seatInfoList[i].userId]) {
                    kickSeatIndex = i;
                    break;
                }
            }
            if (kickSeatIndex != -1) {
                [self kickSeat:kickSeatIndex callback:nil];
            }
        }
    }
}

- (void)onTRTCAudioAvailable:(nonnull NSString *)userId available:(BOOL)available {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onUserMicrophoneMute:mute:)]) {
            [self.delegate onUserMicrophoneMute:userId mute:!available];
        }
    }];
}

- (void)onUserVoiceVolume:(nonnull NSArray<TRTCVolumeInfo *> *)userVolumes totalVolume:(NSInteger)totalVolume {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onUserVolumeUpdate:totalVolume:)]) {
            [self.delegate onUserVolumeUpdate:userVolumes totalVolume:totalVolume];
        }
    }];
}

- (void)onRecvCustomCmdMsgUserId:(NSString *)userId cmdID:(NSInteger)cmdID seq:(UInt32)seq message:(NSData *)message {
    [self.chorusManager onRecvCustomCmdMsgUserId:userId cmdID:cmdID seq:seq message:message];
}

#pragma mark - TXChorusRoomServiceDelegate
- (void)onRoomInfoChange:(nonnull TXChorusRoomInfo *)roomInfo {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if (self.chorusRole == TRTCChorusRoleAudience) {
            [self.chorusManager stopCdnPlay];
            self.rtmpPlayURL = roomInfo.playUrl;
            [self.chorusManager startCdnPlay:self.rtmpPlayURL view:nil];
        }
        if ([roomInfo.roomId intValue] == 0) {
            return;
        }
        ChorusRoomInfo *room = [[ChorusRoomInfo alloc] init];
        room.roomID = [roomInfo.roomId intValue];
        room.ownerId = roomInfo.ownerId;
        room.memberCount = roomInfo.memberCount;
        room.ownerName = roomInfo.ownerName;
        room.coverUrl = roomInfo.cover;
        room.needRequest = roomInfo.needRequest == 1;
        room.roomName = roomInfo.roomName;
        room.rtmpPlayURL = roomInfo.playUrl;
        if ([self canDelegateResponseMethod:@selector(onRoomInfoChange:)]) {
            [self.delegate onRoomInfoChange:room];
        }
    }];
}

- (void)onSeatInfoListChange:(nonnull NSArray<TXChorusSeatInfo *> *)seatInfoList {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        NSMutableArray* roomSeatList = [[NSMutableArray alloc] initWithCapacity:2];
        for (TXChorusSeatInfo* info in seatInfoList) {
            ChorusSeatInfo* seat = [[ChorusSeatInfo alloc] init];
            seat.userId = info.user;
            seat.status = info.status;
            [roomSeatList addObject:seat];
        }
        self.seatInfoList = roomSeatList;
        if ([self canDelegateResponseMethod:@selector(onSeatInfoChange:)]) {
            [self.delegate onSeatInfoChange:roomSeatList];
        }
    }];
}

- (void)onRoomDestroyWithRoomId:(NSString *)roomID {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self exitRoom:nil];
        [self runOnDelegateQueue:^{
            @strongify(self)
            if (!self) {
                return;
            }
            if ([self canDelegateResponseMethod:@selector(onRoomDestroy:)]) {
                [self.delegate onRoomDestroy:roomID];
            }
        }];
    }];
}

- (void)onRoomRecvRoomTextMsg:(NSString *)roomID message:(NSString *)message userInfo:(TXChorusUserInfo *)userInfo {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        ChorusUserInfo* user = [[ChorusUserInfo alloc] init];
        user.userId = userInfo.userId;
        user.userName = userInfo.userName;
        user.userAvatar = userInfo.avatarURL;
        if ([self canDelegateResponseMethod:@selector(onRecvRoomTextMsg:userInfo:)]) {
            [self.delegate onRecvRoomTextMsg:message userInfo:user];
        }
    }];
}

- (void)onRoomRecvRoomCustomMsg:(NSString *)roomID cmd:(NSString *)cmd message:(NSString *)message userInfo:(TXChorusUserInfo *)userInfo {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        ChorusUserInfo* user = [[ChorusUserInfo alloc] init];
        user.userId = userInfo.userId;
        user.userName = userInfo.userName;
        user.userAvatar = userInfo.avatarURL;
        if ([self canDelegateResponseMethod:@selector(onRecvRoomCustomMsg:message:userInfo:)]) {
            [self.delegate onRecvRoomCustomMsg:cmd message:message userInfo:user];
        }
    }];
}

- (void)onRoomAudienceEnter:(TXChorusUserInfo *)userInfo {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        ChorusUserInfo* user = [[ChorusUserInfo alloc] init];
        user.userId = userInfo.userId;
        user.userName = userInfo.userName;
        user.userAvatar = userInfo.avatarURL;
        if ([self canDelegateResponseMethod:@selector(onAudienceEnter:)]) {
            [self.delegate onAudienceEnter:user];
        }
    }];
}

- (void)onRoomAudienceLeave:(TXChorusUserInfo *)userInfo {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        ChorusUserInfo* user = [[ChorusUserInfo alloc] init];
        user.userId = userInfo.userId;
        user.userName = userInfo.userName;
        user.userAvatar = userInfo.avatarURL;
        if ([self canDelegateResponseMethod:@selector(onAudienceExit:)]) {
            [self.delegate onAudienceExit:user];
        }
    }];
}

- (void)onSeatTakeWithIndex:(NSInteger)index userInfo:(TXChorusUserInfo *)userInfo {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        BOOL isSelfEnterSeat = [userInfo.userId isEqualToString:self.userId];
        if (isSelfEnterSeat) {
            // 是自己上线了
            self.takeSeatIndex = index;
            [self.roomTRTCService switchToAnchor];
        }
        [self runOnDelegateQueue:^{
            @strongify(self)
            if (!self) {
                return;
            }
            ChorusUserInfo* user = [[ChorusUserInfo alloc] init];
            user.userId = userInfo.userId;
            user.userName = userInfo.userName;
            user.userAvatar = userInfo.avatarURL;
            if ([self canDelegateResponseMethod:@selector(onAnchorEnterSeat:user:)]) {
                [self.delegate onAnchorEnterSeat:index user:user];
            }
            if (self.pickSeatCallback) {
                self.pickSeatCallback(0, @"pick seat success");
                self.pickSeatCallback = nil;
            }
        }];
        if (isSelfEnterSeat) {
            [self runOnDelegateQueue:^{
                @strongify(self)
                if (!self) {
                    return;
                }
                if (self.enterSeatCallback) {
                    self.enterSeatCallback(0, @"enter seat success.");
                    self.enterSeatCallback = nil;
                }
            }];
        }
    }];
}

- (void)onSeatLeaveWithIndex:(NSInteger)index userInfo:(TXChorusUserInfo *)userInfo {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self.userId isEqualToString:userInfo.userId]) {
            self.takeSeatIndex = -1;
            [self.roomTRTCService switchToAudience];
            if (self.chorusRole == TRTCChorusRoleChorus) {
                [self.roomTRTCService exitRoom:^(int code, NSString * _Nonnull message) {
                    @strongify(self)
                    if (code == 0) {
                        self.chorusRole = TRTCChorusRoleAudience;
                        [self.chorusManager stopChorus];
                    }
                }];
            }
        }
        ChorusUserInfo* user = [[ChorusUserInfo alloc] init];
        user.userId = userInfo.userId;
        user.userName = userInfo.userName;
        user.userAvatar = userInfo.avatarURL;
        if ([self canDelegateResponseMethod:@selector(onAnchorLeaveSeat:user:)]) {
            [self.delegate onAnchorLeaveSeat:index user:user];
        }
        if (self.kickSeatCallback) {
            self.kickSeatCallback(0, @"kick seat success.");
            self.kickSeatCallback = nil;
        }
        if ([self.userId isEqualToString:userInfo.userId]) {
            if (self.leaveSeatCallback) {
                self.leaveSeatCallback(0, @"leave seat success.");
                self.leaveSeatCallback = nil;
            }
        }
    }];
}

- (void)onSeatCloseWithIndex:(NSInteger)index isClose:(BOOL)isClose {
    @weakify(self)
    [self runMainQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if (self.takeSeatIndex == index) {
            [self.roomTRTCService switchToAudience];
            self.takeSeatIndex = -1;
        }
        [self runOnDelegateQueue:^{
            @strongify(self)
            if (!self) {
                return;
            }
            if ([self canDelegateResponseMethod:@selector(onSeatClose:isClose:)]) {
                [self.delegate onSeatClose:index isClose:isClose];
            }
        }];
    }];
}

- (void)onReceiveNewInvitationWithIdentifier:(NSString *)identifier inviter:(NSString *)inviter cmd:(NSString *)cmd content:(NSString *)content {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onReceiveNewInvitation:inviter:cmd:content:)]) {
            [self.delegate onReceiveNewInvitation:identifier inviter:inviter cmd:cmd content:content];
        }
    }];
}

- (void)onInviteeAcceptedWithIdentifier:(NSString *)identifier invitee:(NSString *)invitee {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onInviteeAccepted:invitee:)]) {
            [self.delegate onInviteeAccepted:identifier invitee:invitee];
        }
    }];
}

- (void)onInviteeRejectedWithIdentifier:(NSString *)identifier invitee:(NSString *)invitee {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onInviteeRejected:invitee:)]) {
            [self.delegate onInviteeRejected:identifier invitee:invitee];
        }
    }];
}

- (void)onInviteeCancelledWithIdentifier:(NSString *)identifier invitee:(NSString *)invitee {
    @weakify(self)
    [self runOnDelegateQueue:^{
        @strongify(self)
        if (!self) {
            return;
        }
        if ([self canDelegateResponseMethod:@selector(onInvitationCancelled:invitee:)]) {
            [self.delegate onInvitationCancelled:identifier invitee:invitee];
        }
    }];
}

@end
