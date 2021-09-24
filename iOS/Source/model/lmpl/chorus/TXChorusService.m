//
//  TXChorusService.m
//  Alamofire
//
//  Created by adams on 2021/7/14.
//

#import "TXChorusService.h"
#import "TXChorusCommonDef.h"
#import "ImSDK_Plus.h"
#import "ChorusLocalized.h"
#import "TXChorusIMJsonHandle.h"
#import "MJExtension.h"

@interface TXChorusService()<V2TIMSDKListener, V2TIMGroupListener, V2TIMSimpleMsgListener, V2TIMSignalingListener>
@property (nonatomic, assign) BOOL isInitIMSDK;
@property (nonatomic, assign) BOOL isLogin;
@property (nonatomic, assign) BOOL isEnterRoom;

@property (nonatomic, strong) NSString *mRoomId;
@property (nonatomic, strong) NSString *selfUserId;
@property (nonatomic, strong) NSString *selfUserName;
@property (nonatomic, strong) NSString *ownerUserId;
@property (nonatomic, strong) TXChorusRoomInfo *roomInfo;
@property (nonatomic, strong) NSArray<TXChorusSeatInfo *> *seatInfoList;

@property (nonatomic, strong, readonly)V2TIMManager* imManager;
@end

@implementation TXChorusService
+ (instancetype)sharedInstance {
    static TXChorusService* instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[TXChorusService alloc] init];
    });
    return instance;
}

#pragma mark - public method
- (void)loginWithSdkAppId:(int)sdkAppId
                   userId:(NSString *)userId
                  userSig:(NSString *)userSig
                 callback:(TXChorusCallback)callback {
    if (!self.isInitIMSDK) {
        V2TIMSDKConfig *config = [[V2TIMSDKConfig alloc] init];
        config.logLevel = V2TIM_LOG_ERROR;
        self.isInitIMSDK = [self.imManager initSDK:sdkAppId config:config listener:self];
        if (!self.isInitIMSDK) {
            if (callback) {
                callback(CHORUS_SERVICE_CODE_ERROR, @"init im sdk error.");
            }
            return;
        }
    }
    if (self.isLogin) {
        self.selfUserId = userId;
        if (callback) {
            callback(CHORUS_SERVICE_CODE_ERROR, @"start login fail, you have been login, can not login twice.");
        }
        return;
    }
    @weakify(self)
    [self.imManager login:userId userSig:userSig succ:^{
        @strongify(self)
        if (!self) {
            return;
        }
        self.isLogin = YES;
        self.selfUserId = userId;
        if (callback) {
            callback(0, @"im login success.");
        }
    } fail:^(int code, NSString *desc) {
        if (callback) {
            callback(code, desc ?: @"im login error");
        }
    }];
}

- (void)logout:(TXChorusCallback)callback {
    if (!self.isLogin) {
        if (callback) {
            callback(CHORUS_SERVICE_CODE_ERROR, @"start logout fail. not login yet");
        }
        return;
    }
    if (self.isEnterRoom) {
        if (callback) {
            callback(CHORUS_SERVICE_CODE_ERROR, @"start logout fail. you are in room, please exit room before logout");
        }
        return;
    }
    @weakify(self)
    [self.imManager logout:^{
        @strongify(self)
        if (!self) {
            return;
        }
        self.isLogin = false;
    } fail:^(int code, NSString *desc) {
        
    }];
}

- (void)getSelfInfo{
    if (!self.selfUserId || [self.selfUserId isEqualToString:@""]) {
        return;
    }
    @weakify(self)
    [self.imManager getUsersInfo:@[self.selfUserId] succ:^(NSArray<V2TIMUserFullInfo *> *infoList) {
        @strongify(self)
        if (!self) { return; }
        if (infoList.count > 0) {
            self.selfUserName = infoList.firstObject.nickName ?: @"";
        }
    } fail:^(int code, NSString *desc) {
        TRTCLog(@"get self info fail,code: %d reason: %@",code, desc);
    }];
}

- (void)setSelfProfileWithUserName:(NSString *)userName
                         avatarUrl:(NSString *)avatarUrl
                          callback:(TXChorusCallback _Nullable)callback {
    if (!self.isLogin) {
        if (callback) {
            callback(CHORUS_SERVICE_CODE_ERROR, @"set profile fail, not login yet.");
        }
        return;
    }
    V2TIMUserFullInfo *userInfo = [[V2TIMUserFullInfo alloc] init];
    userInfo.nickName = userName;
    userInfo.faceURL = avatarUrl;
    [self.imManager setSelfInfo:userInfo succ:^{
        if (callback) {
            callback(0, @"set profile success");
        }
    } fail:^(int code, NSString *desc) {
        if (callback) {
            callback(0, desc ?: @"set profile failed.");
        }
    }];
}

- (void)createRoomWithRoomId:(NSString *)roomId
                    roomName:(NSString *)roomName
                    coverUrl:(NSString *)coverUrl
                     playUrl:(NSString *)playUrl
                 needRequest:(BOOL)needRequest
                seatInfoList:(NSArray<TXChorusSeatInfo *> *)seatInfoList
                    callback:(TXChorusCallback _Nullable)callback {
    if (!self.isLogin) {
        if (callback) {
            callback(CHORUS_SERVICE_CODE_ERROR, @"im not login yet, create room fail");
        }
        return;
    }
    if (self.isEnterRoom) {
        if (callback) {
            callback(CHORUS_SERVICE_CODE_ERROR, @"you have been in room");
        }
        return;
    }
    self.mRoomId = roomId;
    self.ownerUserId = self.selfUserId;
    self.seatInfoList = seatInfoList;
    self.roomInfo = [[TXChorusRoomInfo alloc] init];
    self.roomInfo.ownerId = self.selfUserId;
    self.roomInfo.ownerName = self.selfUserName;
    self.roomInfo.roomName = roomName;
    self.roomInfo.playUrl = playUrl;
    self.roomInfo.cover = coverUrl;
    self.roomInfo.seatSize = seatInfoList.count;
    self.roomInfo.needRequest = needRequest ? 1 : 0;
    @weakify(self)
    [self.imManager createGroup:@"AVChatRoom" groupID:roomId groupName:roomName succ:^(NSString *groupID) {
        @strongify(self)
        if (!self) {
            return;
        }
        [self setGroupInfoWithRoomId:roomId roomName:roomName coverUrl:coverUrl userName:self.selfUserName];
        [self onCreateSuccess:callback];
    } fail:^(int code, NSString *desc) {
        @strongify(self)
        if (!self) {
            return;
        }
        TRTCLog(@"create room error: %d, msg: %@", code, desc);
        NSString *msg = desc ?: @"create room fiald";
        if (code == 10036) {
            msg = LocalizeReplaceXX(ChorusLocalize(@"Demo.TRTC.Buy.chatroom"), @"https://cloud.tencent.com/document/product/269/11673");
        } else if (code == 10037) {
            msg = LocalizeReplaceXX(ChorusLocalize(@"Demo.TRTC.Buy.grouplimit"), @"https://cloud.tencent.com/document/product/269/11673");
        } else if (code == 10038) {
            msg = LocalizeReplaceXX(ChorusLocalize(@"Demo.TRTC.Buy.groupmemberlimit"), @"https://cloud.tencent.com/document/product/269/11673");
        }
        
        if (code == 10025 || code == 10021) {
            // 表明群主是自己，认为创建成功
            // 群ID已被他人使用，走进房的逻辑
            [self setGroupInfoWithRoomId:roomId roomName:roomName coverUrl:coverUrl userName:self.selfUserName];
            [self.imManager joinGroup:roomId msg:@"" succ:^{
                TRTCLog(@"gorup has benn created. join group success");
                [self onCreateSuccess:callback];
            } fail:^(int code, NSString *desc) {
                TRTCLog(@"error: group has been created. join group fail. code:%d, message: %@", code, desc);
                if (callback) {
                    callback(code, desc ?: @"");
                }
            }];
        } else {
            if (callback) {
                callback(code, msg);
            }
        }
    }];
}

- (void)destroyRoom:(TXChorusCallback _Nullable)callback {
    if (!self.isOwner) {
        if (callback) {
            callback(-1, @"only owner could destroy room");
        }
        return;
    }
    @weakify(self)
    [self.imManager dismissGroup:self.mRoomId succ:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self unInitIMListener];
        [self cleanRoomStatus];
        if (callback) {
            callback(0, @"destroy room success.");
        }
    } fail:^(int code, NSString *desc) {
        @strongify(self)
        if (!self) {
            return;
        }
        if (code == 10007) {
            TRTCLog(@"your are not real owner, start logic destroy.");
            [self cleanGroupAttr];
            [self sendGroupMsg:[TXChorusIMJsonHandle getRoomdestroyMsg] callback:callback];
            [self unInitIMListener];
            [self cleanRoomStatus];
        } else {
            if (callback) {
                callback(code, desc ?: @"destroy room failed");
            }
        }
    }];
}

- (void)enterRoom:(NSString *)roomId callback:(TXChorusCallback _Nullable)callback {
    [self cleanRoomStatus];
    self.mRoomId = roomId;
    @weakify(self)
    [self.imManager joinGroup:roomId msg:@"" succ:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self onJoinRoomSuccessWithRoomId:roomId callback:callback];
    } fail:^(int code, NSString *desc) {
        @strongify(self)
        if (!self) {
            return;
        }
        if (code == 10013) {
            [self onJoinRoomSuccessWithRoomId:roomId callback:callback];
        } else {
            if (callback) {
                callback(-1, [NSString stringWithFormat:@"join group eror, enter room fail. code:%d, msg:%@", code ,desc]);
            }
        }
    }];
}

- (void)exitRoom:(TXChorusCallback _Nullable)callback {
    if (!self.isEnterRoom) {
        if (callback) {
            callback(-1,@"not enter room yet, can't exit room.");
        }
        return;
    }
    @weakify(self)
    [self.imManager quitGroup:self.mRoomId succ:^{
        @strongify(self)
        if (!self) {
            return;
        }
        [self unInitIMListener];
        [self cleanRoomStatus];
        if (callback) {
            callback(0, @"exite room success.");
        }
    } fail:^(int code, NSString *desc) {
        @strongify(self)
        if (!self) {
            return;
        }
        [self unInitIMListener];
        if (callback) {
            callback(code, desc ?: @"exite room failed.");
        }
    }];
}

- (void)takeSeat:(NSInteger)seatIndex callback:(TXChorusCallback _Nullable)callback {
    if (!callback) {
        callback = ^(int code, NSString* message){
            
        };
    }
    if (seatIndex >=0 && seatIndex < self.seatInfoList.count) {
        TXChorusSeatInfo* info = self.seatInfoList[seatIndex];
        if (info.status == kTXChorusSeatStatusUsed) {
            callback(-1, @"seat is used");
            return;
        }
        if (info.status == kTXChorusSeatStatusClosed) {
            callback(-1, @"seat is closed.");
            return;
        }
        TXChorusSeatInfo* changeInfo = [[TXChorusSeatInfo alloc] init];
        changeInfo.status = kTXChorusSeatStatusUsed;
        changeInfo.user = self.selfUserId;
        NSDictionary *dic = [TXChorusIMJsonHandle getSeatInfoJsonStrWithIndex:seatIndex info:changeInfo];
        [self modeifyGroupAttrs:dic callback:callback];
    } else {
        if (callback) {
            callback(-1, @"seat info list is empty or index error.");
        }
    }
}

- (void)leaveSeat:(NSInteger)seatIndex callback:(TXChorusCallback _Nullable)callback {
    if (!callback) {
        callback = ^(int code, NSString* message){
            
        };
    }
    if (seatIndex >=0 && seatIndex < self.seatInfoList.count) {
        TXChorusSeatInfo* info = self.seatInfoList[seatIndex];
        if (![self.selfUserId isEqualToString:info.user]) {
            callback(-1, @"not in the seat");
            return;
        }
        TXChorusSeatInfo* changeInfo = [[TXChorusSeatInfo alloc] init];
        changeInfo.status = kTXChorusSeatStatusUnused;
        changeInfo.user = @"";
        NSDictionary *dic = [TXChorusIMJsonHandle getSeatInfoJsonStrWithIndex:seatIndex info:changeInfo];
        [self modeifyGroupAttrs:dic callback:callback];
    } else {
        if (callback) {
            callback(-1, @"seat info list is empty or index error.");
        }
    }
}

- (void)pickSeat:(NSInteger)seatIndex userId:(NSString *)userId callback:(TXChorusCallback)callback {
    if (!callback) {
        callback = ^(int code, NSString* message){};
    }
    if (!self.isOwner) {
        callback(-1, @"seat info list is empty or index error.");
        return;
    }
    if (seatIndex < 0 || seatIndex >= self.seatInfoList.count) {
        callback(-1, @"seat info list is empty or index error.");
        return;
    }
    TXChorusSeatInfo *info = self.seatInfoList[seatIndex];
    if (info.status == kTXChorusSeatStatusUsed) {
        callback(-1, @"seat status is used");
        return;
    }
    if (info.status == kTXChorusSeatStatusClosed) {
        callback(-1, @"seat status is close");
        return;
    }
    TXChorusSeatInfo *changeInfo = [[TXChorusSeatInfo alloc] init];
    changeInfo.status = kTXChorusSeatStatusUsed;
    changeInfo.user = userId;
    NSDictionary *dic = [TXChorusIMJsonHandle getSeatInfoJsonStrWithIndex:seatIndex info:changeInfo];
    [self modeifyGroupAttrs:dic callback:callback];
}

- (void)kickSeat:(NSInteger)seatIndex callback:(TXChorusCallback _Nullable)callback {
    if (!callback) {
        callback = ^(int code, NSString* message){};
    }
    if (!self.isOwner) {
        callback(-1, @"seat info list is empty or index error.");
        return;
    }
    if (seatIndex < 0 || seatIndex >= self.seatInfoList.count) {
        callback(-1, @"seat info list is empty or index error.");
        return;
    }
    TXChorusSeatInfo *changeInfo = [[TXChorusSeatInfo alloc] init];
    changeInfo.status = kTXChorusSeatStatusUnused;
    changeInfo.user = @"";
//    changeInfo.mute = self.seatInfoList[seatIndex].mute;
    NSDictionary *dic = [TXChorusIMJsonHandle getSeatInfoJsonStrWithIndex:seatIndex info:changeInfo];
    [self modeifyGroupAttrs:dic callback:callback];
}

- (void)closeSeat:(NSInteger)seatIndex isClose:(BOOL)isClose callback:(TXChorusCallback)callback {
    if (!callback) {
        callback = ^(int code, NSString* message){};
    }
    if (!self.isOwner) {
        callback(-1, @"seat info list is empty or index error.");
        return;
    }
    if (seatIndex < 0 || seatIndex >= self.seatInfoList.count) {
        callback(-1, @"seat info list is empty or index error.");
        return;
    }
    TXChorusSeatInfo *info = self.seatInfoList[seatIndex];
    if (info.status == kTXChorusSeatStatusUsed) {
        callback(-1, @"seat is used, can't closed.");
        return;
    }
    if (info.status == isClose ? kTXChorusSeatStatusClosed : kTXChorusSeatStatusUnused) {
        callback(-1, [NSString stringWithFormat:@"seat is already %@", isClose ? @"close" : @"open"]);
        return;
    }
    TXChorusSeatInfo *changeInfo = [[TXChorusSeatInfo alloc] init];
    changeInfo.status = isClose ? kTXChorusSeatStatusClosed : kTXChorusSeatStatusUnused;
    changeInfo.user = @"";
    NSDictionary *dic = [TXChorusIMJsonHandle getSeatInfoJsonStrWithIndex:seatIndex info:changeInfo];
    [self modeifyGroupAttrs:dic callback:callback];
}

- (void)getRoomInfoList:(NSArray<NSString *> *)roomIds calback:(TXChorusRoomInfoListCallback _Nullable)callback {
    [self.imManager getGroupsInfo:roomIds succ:^(NSArray<V2TIMGroupInfoResult *> *groupResultList) {
        if (groupResultList) {
            NSMutableArray *groupResults = [[NSMutableArray alloc] initWithCapacity:2];
            NSMutableDictionary *tempDic = [[NSMutableDictionary alloc] initWithCapacity:2];
            [groupResultList enumerateObjectsUsingBlock:^(V2TIMGroupInfoResult * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                if (obj && obj.info.groupID) {
                    tempDic[obj.info.groupID] = obj;
                }
            }];
            [roomIds enumerateObjectsUsingBlock:^(NSString * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                TXChorusRoomInfo *roomInfo = [[TXChorusRoomInfo alloc] init];
                V2TIMGroupInfoResult* groupInfo = tempDic[obj];
                if (groupInfo) {
                    roomInfo.roomId = groupInfo.info.groupID;
                    roomInfo.cover = groupInfo.info.faceURL;
                    roomInfo.memberCount = groupInfo.info.memberCount;
                    roomInfo.ownerId = groupInfo.info.owner;
                    roomInfo.roomName = groupInfo.info.groupName;
                    roomInfo.ownerName = groupInfo.info.introduction;
                }
                [groupResults addObject:roomInfo];
            }];
            if (callback) {
                callback(0, @"success.", groupResults);
            }
        } else {
            if (callback) {
                callback(-1, @"get group info failed.reslut is nil.", @[]);
            }
        }
    } fail:^(int code, NSString *desc) {
        
    }];
}

- (void)getUserInfo:(NSArray<NSString *> *)userList callback:(TXChorusUserListCallback _Nullable)callback {
    if (!self.isEnterRoom) {
        if (callback) {
            callback(CHORUS_SERVICE_CODE_ERROR, @"get user info list fail, not enter room yet", @[]);
        }
        return;
    }
    if (!userList || userList.count == 0) {
        if (callback) {
            callback(CHORUS_SERVICE_CODE_ERROR, @"get user info list fail, user id list is empty.", @[]);
        }
        return;
    }
    [self.imManager getUsersInfo:userList succ:^(NSArray<V2TIMUserFullInfo *> *infoList) {
        NSMutableArray *txUserInfo = [[NSMutableArray alloc] initWithCapacity:2];
        [infoList enumerateObjectsUsingBlock:^(V2TIMUserFullInfo * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            TXChorusUserInfo *userInfo = [[TXChorusUserInfo alloc] init];
            userInfo.userName = obj.nickName ?: @"";
            userInfo.userId = obj.userID ?: @"";
            userInfo.avatarURL = obj.faceURL ?: @"";
            [txUserInfo addObject:userInfo];
        }];
        if (callback) {
            callback(0, @"success", txUserInfo);
        }
    } fail:^(int code, NSString *desc) {
        if (callback) {
            callback(code, desc ?: @"get user info failed", @[]);
        }
    }];
}

- (void)getAudienceList:(TXChorusUserListCallback _Nullable)callback {
    [self.imManager getGroupMemberList:self.mRoomId filter:V2TIM_GROUP_MEMBER_FILTER_COMMON nextSeq:0 succ:^(uint64_t nextSeq, NSArray<V2TIMGroupMemberFullInfo *> *memberList) {
        if (memberList) {
            NSMutableArray *resultList = [[NSMutableArray alloc] initWithCapacity:2];
            [memberList enumerateObjectsUsingBlock:^(V2TIMGroupMemberFullInfo * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                TXChorusUserInfo *info = [[TXChorusUserInfo alloc] init];
                info.userId = obj.userID;
                info.userName = obj.nickName;
                info.avatarURL = obj.faceURL;
                [resultList addObject:info];
            }];
            if (callback) {
                callback(0, @"get audience list success.", resultList);
            }
        } else {
            if (callback) {
                callback(-1, @"get audience list fail, results is nil", @[]);
            }
        }
    } fail:^(int code, NSString *desc) {
        if (callback) {
            callback(code, desc ?: @"get sudience list fail.", @[]);
        }
    }];
}

- (void)destroy {
    
}

#pragma mark - V2TIMSimpleMsgListener
- (void)onRecvC2CTextMessage:(NSString *)msgID sender:(V2TIMUserInfo *)info text:(NSString *)text {
    
}

- (void)onRecvC2CCustomMessage:(NSString *)msgID sender:(V2TIMUserInfo *)info customData:(NSData *)data {
    
}

- (void)onRecvGroupTextMessage:(NSString *)msgID groupID:(NSString *)groupID sender:(V2TIMGroupMemberInfo *)info text:(NSString *)text {
    TRTCLog(@"im get tet msg group:%@, userId:%@, text:%@", groupID, info.userID, text);
    if (![groupID isEqualToString:self.mRoomId]) {
        return;
    }
    TXChorusUserInfo* userInfo = [[TXChorusUserInfo alloc] init];
    userInfo.userId = info.userID;
    userInfo.avatarURL = info.faceURL;
    userInfo.userName = info.nickName;
    if ([self canDelegateResponseMethod:@selector(onRoomRecvRoomTextMsg:message:userInfo:)]) {
        [self.delegate onRoomRecvRoomTextMsg:self.mRoomId message:text userInfo:userInfo];
    }
}

- (void)onRecvGroupCustomMessage:(NSString *)msgID groupID:(NSString *)groupID sender:(V2TIMGroupMemberInfo *)info customData:(NSData *)data {
    TRTCLog(@"im get custom msg group:%@, userId:%@, text:%@", groupID, info.userID, data);
    if (![groupID isEqualToString:self.mRoomId]) {
        return;
    }
    if (!data) {
        return;
    }
    NSString* jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    NSDictionary* dic = [jsonString mj_JSONObject];
    NSString *version = [dic objectForKey:CHORUS_KEY_VERSION];
    if (!version || ![version isEqualToString:CHORUS_VALUE_VERSION_STRING]) {
        TRTCLog(@"protocol version is not match, ignore msg");
        return;
    }
    NSNumber* action = [dic objectForKey:CHORUS_KEY_ACTION];
    if (!action) {
        TRTCLog(@"action can't parse from data");
        return;
    }
    int actionValue = [action intValue];
    switch (actionValue) {
        case kChorusCodeUnknown:
            break;
        case kChorusCodeCustomMsg:
        {
            NSDictionary *cusPair = [TXChorusIMJsonHandle parseCusMsgWithJsonDic:dic];
            TXChorusUserInfo *userInfo = [[TXChorusUserInfo alloc] init];
            userInfo.userId = info.userID;
            userInfo.avatarURL = info.faceURL;
            userInfo.userName = info.nickName;
            if ([self canDelegateResponseMethod:@selector(onRoomRecvRoomCustomMsg:cmd:message:userInfo:)]) {
                [self.delegate onRoomRecvRoomCustomMsg:self.mRoomId cmd:cusPair[CHORUS_KEY_CMD] message:cusPair[CHORUS_KEY_MESSAGE] userInfo:userInfo];
            }
        }
            break;
        case kChorusCodeDestroy:
        {
            [self exitRoom:nil];
            if ([self canDelegateResponseMethod:@selector(onRoomDestroyWithRoomId:)]) {
                [self.delegate onRoomDestroyWithRoomId:self.mRoomId];
            }
            [self cleanRoomStatus];
        }
            break;
        default:
            break;
    }
}
#pragma mark - V2TIMGroupListener
- (void)onMemberEnter:(NSString *)groupID memberList:(NSArray<V2TIMGroupMemberInfo *> *)memberList{
    if (![groupID isEqualToString:self.mRoomId]) {
        return;
    }
    [memberList enumerateObjectsUsingBlock:^(V2TIMGroupMemberInfo * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        TXChorusUserInfo* userInfo = [[TXChorusUserInfo alloc] init];
        userInfo.userId = obj.userID;
        userInfo.avatarURL = obj.faceURL;
        userInfo.userName = obj.nickName;
        if ([self canDelegateResponseMethod:@selector(onRoomAudienceEnter:)]) {
            [self.delegate onRoomAudienceEnter:userInfo];
        }
    }];
}

- (void)onMemberLeave:(NSString *)groupID member:(V2TIMGroupMemberInfo *)member{
    if (![groupID isEqualToString:self.mRoomId]) {
        return;
    }
    if (!member) {
        return;
    }
    TXChorusUserInfo *userInfo = [[TXChorusUserInfo alloc] init];
    userInfo.userId = member.userID;
    userInfo.avatarURL = member.faceURL;
    userInfo.userName = member.nickName;
    if ([self canDelegateResponseMethod:@selector(onRoomAudienceLeave:)]) {
        [self.delegate onRoomAudienceLeave:userInfo];
    }
}

- (void)onGroupDismissed:(NSString *)groupID opUser:(V2TIMGroupMemberInfo *)opUser{
    if (![groupID isEqualToString:self.mRoomId]) {
        return;
    }
    [self cleanRoomStatus];
    if ([self canDelegateResponseMethod:@selector(onRoomDestroyWithRoomId:)]) {
        [self.delegate onRoomDestroyWithRoomId:groupID];
    }
}

- (void)onGroupAttributeChanged:(NSString *)groupID attributes:(NSMutableDictionary<NSString *,NSString *> *)attributes {
    TRTCLog(@"on group attr changed:%@", attributes);
    if (![groupID isEqualToString:self.mRoomId]) {
        return;
    }
    if (self.roomInfo.seatSize == 0) {
        TRTCLog(@"group attr changed, but room info is empty");
        return;
    }
    if (!attributes) {
        TRTCLog(@"attributes error");
        return;
    }
    NSArray<TXChorusSeatInfo *> *seatInfoList = [TXChorusIMJsonHandle getSeatListFromAttr:attributes seatSize:self.roomInfo.seatSize];
    NSArray<TXChorusSeatInfo *> *oldSeatInfoList = [self.seatInfoList copy];
    TXChorusRoomInfo *roomInfo = [TXChorusIMJsonHandle getRoomInfoFromAttr:attributes];
    if (roomInfo) {
        if ([self canDelegateResponseMethod:@selector(onRoomInfoChange:)]) {
            [self.delegate onRoomInfoChange:roomInfo];
        }
    }
    self.seatInfoList = [seatInfoList mutableCopy];
    if ([self canDelegateResponseMethod:@selector(onSeatInfoListChange:)]) {
        [self.delegate onSeatInfoListChange:self.seatInfoList];
    }
    for (int i = 0; i < self.roomInfo.seatSize; i+=1) {
        TXChorusSeatInfo *old = oldSeatInfoList[i];
        TXChorusSeatInfo *new = self.seatInfoList[i];
        if (old.status != new.status) {
            switch (new.status) {
                case kTXChorusSeatStatusUnused:
                    if (old.status == kTXChorusSeatStatusClosed) {
                        [self onSeatcloseWithIndex:i isClose:NO];
                    } else {
                        [self onSeatLeaveWithIndex:i user:old.user];
                    }
                    break;
                case kTXChorusSeatStatusUsed:
                    [self onSeatTakeWithIndex:i user:new.user];
                    break;
                case kTXChorusSeatStatusClosed:
                    [self onSeatcloseWithIndex:i isClose:YES];
                    break;
                default:
                    break;
            }
        }
    }
}


#pragma mark - V2TIMSignalingListener
- (void)onReceiveNewInvitation:(NSString *)inviteID inviter:(NSString *)inviter groupID:(NSString *)groupID inviteeList:(NSArray<NSString *> *)inviteeList data:(NSString *)data{
    NSDictionary *dic = [data mj_JSONObject];
    if (![dic isKindOfClass:[NSDictionary class]]) {
        TRTCLog(@"parse data error");
        return;
    }
    NSInteger version = [[dic objectForKey:CHORUS_KEY_VERSION] integerValue];
    if (version < CHORUS_VALUE_BASIC_VERSION) {
        TRTCLog(@"protocol version is nil or not match, ignore c2c msg");
        return;
    }
    NSString *businessID = [dic objectForKey:CHORUS_KEY_BUSINESS_ID];
    if (!businessID || ![businessID isEqualToString:CHORUS_VALUE_BUSINESS_ID]) {
        TRTCLog(@"bussiness id error");
        return;
    }
    
    NSDictionary *cmdData = [dic objectForKey:CHORUS_KEY_DATA];
    NSString *cmd = [cmdData objectForKey:CHORUS_KEY_CMD];
    NSString *content = [cmdData objectForKey:CHORUS_KEY_SEATNUMBER];
    int roomID = [[cmdData objectForKey:CHORUS_KEY_ROOM_ID] intValue];
    if ([self.mRoomId intValue] != roomID) {
        TRTCLog(@"room id is not right");
        return;
    }
    if ([self canDelegateResponseMethod:@selector(onReceiveNewInvitationWithIdentifier:inviter:cmd:content:)]) {
        [self.delegate onReceiveNewInvitationWithIdentifier:inviteID inviter:inviter cmd:cmd content:content];
    }
}

- (void)onInviteeAccepted:(NSString *)inviteID invitee:(NSString *)invitee data:(NSString *)data {
    if ([self canDelegateResponseMethod:@selector(onInviteeAcceptedWithIdentifier:invitee:)]) {
        [self.delegate onInviteeAcceptedWithIdentifier:inviteID invitee:invitee];
    }
}

-(void)onInviteeRejected:(NSString *)inviteID invitee:(NSString *)invitee data:(NSString *)data {
    if ([self canDelegateResponseMethod:@selector(onInviteeRejectedWithIdentifier:invitee:)]) {
        [self.delegate onInviteeRejectedWithIdentifier:inviteID invitee:invitee];
    }
}

- (void)onInvitationCancelled:(NSString *)inviteID inviter:(NSString *)inviter data:(NSString *)data {
    if ([self canDelegateResponseMethod:@selector(onInviteeCancelledWithIdentifier:invitee:)]) {
        [self.delegate onInviteeCancelledWithIdentifier:inviteID invitee:inviter];
    }
}

#pragma mark - private method

- (V2TIMManager *)imManager {
    return [V2TIMManager sharedInstance];
}

- (BOOL)isOwner {
    return [self.selfUserId isEqualToString:self.ownerUserId];
}

- (void)cleanRoomStatus {
    self.isEnterRoom = NO;
    self.mRoomId = @"";
    self.ownerUserId = @"";
}

- (BOOL)canDelegateResponseMethod:(SEL)method {
    return self.delegate && [self.delegate respondsToSelector:method];
}

- (void)setGroupInfoWithRoomId:(NSString *)roomId roomName:(NSString *)roomName coverUrl:(NSString *)coverUrl userName:(NSString *)userName {
    V2TIMGroupInfo *info = [[V2TIMGroupInfo alloc] init];
    info.groupID = roomId;
    info.groupName = roomName;
    info.faceURL = coverUrl;
    info.introduction = userName;
    [self.imManager setGroupInfo:info succ:^{
        TRTCLog(@"success: set group info success.");
    } fail:^(int code, NSString *desc) {
        TRTCLog(@"fail: set group info fail.");
    }];
}

- (void)onCreateSuccess:(TXChorusCallback _Nullable)callback {
    [self initImListener];
    @weakify(self)
    [self.imManager initGroupAttributes:self.mRoomId
                             attributes:[TXChorusIMJsonHandle getInitRoomDicWithRoomInfo:self.roomInfo seatInfoList:self.seatInfoList]
                                   succ:^{
        @strongify(self)
        if (!self) { return; }
        self.isEnterRoom = YES;
        if (callback) {
            callback(0, @"init room info and seat success");
        }
    } fail:^(int code, NSString *desc) {
        @strongify(self)
        if (!self) { return; }
        if (code == 10007) {
            [self.imManager joinGroup:self.mRoomId msg:@"" succ:^{
                TRTCLog(@"gorup has benn created. join group success");
                @strongify(self)
                if (!self) { return; }
                self.isEnterRoom = YES;
                if (callback) {
                    callback(0, @"init room info and seat success");
                }
            } fail:^(int code, NSString *desc) {
                TRTCLog(@"error: group has been created. join group fail. code:%d, message: %@", code, desc);
                if (callback) {
                    callback(code, desc ?: @"init group attributes failed");
                }
            }];
        }
    }];
}

- (void)initImListener {
    [self.imManager setGroupListener:self];
    // 设置前先remove下，防止在单例的情况下重复设置
    [self.imManager removeSignalingListener:self];
    [self.imManager removeSimpleMsgListener:self];
    [self.imManager addSignalingListener:self];
    [self.imManager addSimpleMsgListener:self];
}

- (void)unInitIMListener {
    [self.imManager setGroupListener:nil];
    [self.imManager removeSignalingListener:self];
    [self.imManager removeSimpleMsgListener:self];
}

- (void)cleanGroupAttr {
    [self.imManager deleteGroupAttributes:self.mRoomId keys:nil succ:nil fail:nil];
}

- (void)sendGroupMsg:(NSString *)message callback:(TXChorusCallback)callback {
    if (!self.mRoomId || [self.mRoomId isEqualToString:@""]) {
        if (callback) {
            callback(-1, @"gourp id is wrong.please check it.");
        }
        return;
    }
    NSData *data = [message dataUsingEncoding:NSUTF8StringEncoding];
    if (!data) {
        callback(-1, @"message can't covert to data");
        return;
    }
    [self.imManager sendGroupCustomMessage:data to:self.mRoomId priority:V2TIM_PRIORITY_NORMAL succ:^{
        if (callback) {
            callback(0, @"send group message success.");
        }
    } fail:^(int code, NSString *desc) {
        TRTCLog(@"error: send group message error. error:%d, message:%@", code, desc);
        if (callback) {
            callback(code, desc);
        }
    }];
}

- (void)onJoinRoomSuccessWithRoomId:(NSString *)roomId callback:(TXChorusCallback _Nullable)callback {
    @weakify(self)
    [self.imManager getGroupAttributes:roomId keys:nil succ:^(NSMutableDictionary<NSString *,NSString *> *groupAttributeList) {
        @strongify(self)
        if (!self) {
            return;
        }
        [self initImListener];
        if (!groupAttributeList) {
            return;
        }
        // 解析roomInfo
        TXChorusRoomInfo* roomInfo = [TXChorusIMJsonHandle getRoomInfoFromAttr:groupAttributeList];
        if (roomInfo) {
            roomInfo.roomId = roomId;
            roomInfo.memberCount = -1; // 当前房间的MemberCount无法从这个接口正确获取。
            self.roomInfo = roomInfo;
        } else {
            TRTCLog(@"group room info is empty, enter room failed.");
            if (callback) {
                callback(-1, @"group room info is empty, enter room failed.");
            }
            return;
        }
        TRTCLog(@"enter room successed.");
        self.mRoomId = roomId;
        self.seatInfoList = [TXChorusIMJsonHandle getSeatListFromAttr:groupAttributeList seatSize:self.roomInfo.seatSize];
        self.isEnterRoom = true;
        self.ownerUserId = self.roomInfo.ownerId;
        if ([self canDelegateResponseMethod:@selector(onRoomInfoChange:)]) {
            [self.delegate onRoomInfoChange:self.roomInfo];
        }
        if ([self canDelegateResponseMethod:@selector(onSeatInfoListChange:)]) {
            [self.delegate onSeatInfoListChange:self.seatInfoList];
        }
        if (callback) {
            callback(0, @"enter rooom success");
        }
    } fail:^(int code, NSString *desc) {
        if (callback) {
            callback(code, desc ?: @"get group attr error");
        }
    }];
}

- (void)modeifyGroupAttrs:(NSDictionary<NSString *, NSString *> *)attrs callback:(TXChorusCallback _Nullable)callback {
    [self.imManager setGroupAttributes:self.mRoomId attributes:attrs succ:^{
        if (callback) {
            callback(0, @"modify group attrs success");
        }
    } fail:^(int code, NSString *desc) {
        if (callback) {
            callback(code, desc ?: @"modify group attrs failed");
        }
    }];
}

- (void)sendRoomTextMsg:(NSString *)msg callback:(TXChorusCallback)callback {
    if (!self.isEnterRoom) {
        if (callback) {
            callback(-1, @"send room text fail. not enter room yet.");
        }
        return;
    }
    [self.imManager sendGroupTextMessage:msg to:self.mRoomId priority:V2TIM_PRIORITY_NORMAL succ:^{
        if (callback) {
            callback(0, @"send gourp message success.");
        }
    } fail:^(int code, NSString *desc) {
        if (callback) {
            callback(code, desc ?: @"send group message error.");
        }
    }];
}

- (void)sendRoomCustomMsg:(NSString *)cmd message:(NSString *)message callback:(TXChorusCallback)callback {
    if (!self.isEnterRoom) {
        if (callback) {
            callback(-1, @"send room text fail. not enter room yet.");
        }
        return;
    }
    [self sendGroupMsg:[TXChorusIMJsonHandle getCusMsgJsonStrWithCmd:cmd msg:message] callback:callback];
}

- (NSString *)sendInvitation:(NSString *)cmd userId:(NSString *)userId content:(NSString *)content callback:(TXChorusCallback _Nullable)callback {
    NSDictionary *dic = @{
        CHORUS_KEY_VERSION:@(CHORUS_VALUE_VERSION),
        CHORUS_KEY_BUSINESS_ID:CHORUS_VALUE_BUSINESS_ID,
        CHORUS_KEY_PLATFORM:CHORUS_VALUE_PLATFORM,
        CHORUS_KEY_EXTRAINFO:@"",
        CHORUS_KEY_DATA:@{
                CHORUS_KEY_ROOM_ID:@(self.mRoomId.intValue),
                CHORUS_KEY_CMD:cmd,
                CHORUS_KEY_SEATNUMBER:content,
        },
    };
    NSString *jsonString = [dic mj_JSONString];
    return [self.imManager invite:userId data:jsonString onlineUserOnly:YES offlinePushInfo:nil timeout:0 succ:^{
        TRTCLog(@"send invitation success.");
        if (callback) {
            callback(0, @"send invitation success.");
        }
    } fail:^(int code, NSString *desc) {
        TRTCLog(@"send invitation failed");
        if (callback) {
            callback(code, desc ?: @"send invatiaon failed");
        }
    }];
}

- (void)acceptInvitation:(NSString *)identifier callback:(TXChorusCallback _Nullable)callback {
    TRTCLog(@"accept %@", identifier);
    NSDictionary *dic = @{
        CHORUS_KEY_VERSION:@(CHORUS_VALUE_VERSION),
        CHORUS_KEY_BUSINESS_ID:CHORUS_VALUE_BUSINESS_ID,
        CHORUS_KEY_PLATFORM:CHORUS_VALUE_PLATFORM,
    };
    NSString *jsonString = [dic mj_JSONString];
    [self.imManager accept:identifier data:jsonString succ:^{
        TRTCLog(@"accept invitation success.");
        if (callback) {
            callback(0, @"accept invitation success.");
        }
    } fail:^(int code, NSString *desc) {
        TRTCLog(@"accept invitation failed");
        if (callback) {
            callback(code, desc ?: @"accept invatiaon failed");
        }
    }];
}

- (void)rejectInvitaiton:(NSString *)identifier callback:(TXChorusCallback _Nullable)callback {
    TRTCLog(@"reject %@", identifier);
    NSDictionary *dic = @{
        CHORUS_KEY_VERSION:@(CHORUS_VALUE_VERSION),
        CHORUS_KEY_BUSINESS_ID:CHORUS_VALUE_BUSINESS_ID,
        CHORUS_KEY_PLATFORM:CHORUS_VALUE_PLATFORM,
    };
    NSString *jsonString = [dic mj_JSONString];
    [self.imManager reject:identifier data:jsonString succ:^{
        TRTCLog(@"reject invitation success.");
        if (callback) {
            callback(0, @"reject invitation success.");
        }
    } fail:^(int code, NSString *desc) {
        TRTCLog(@"reject invitation failed");
        if (callback) {
            callback(code, desc ?: @"reject invatiaon failed");
        }
    }];
}

- (void)cancelInvitation:(NSString *)identifier callback:(TXChorusCallback _Nullable)callback {
    TRTCLog(@"cancel %@", identifier);
    NSDictionary *dic = @{
        CHORUS_KEY_VERSION:@(CHORUS_VALUE_VERSION),
        CHORUS_KEY_BUSINESS_ID:CHORUS_VALUE_BUSINESS_ID,
        CHORUS_KEY_PLATFORM:CHORUS_VALUE_PLATFORM,
    };
    NSString *jsonString = [dic mj_JSONString];
    [self.imManager cancel:identifier data:jsonString succ:^{
        TRTCLog(@"cancel invitation success.");
        if (callback) {
            callback(0, @"cancel invitation success.");
        }
    } fail:^(int code, NSString *desc) {
        TRTCLog(@"cancel invitation success.");
        if (callback) {
            callback(0, @"cancel invitation success.");
        }
    }];
}

- (void)onSeatTakeWithIndex:(NSInteger)index user:(NSString *)userId {
    TRTCLog(@"onSeatTake: %ld, user: %@", (long)index, userId);
    @weakify(self)
    [self getUserInfo:@[userId] callback:^(int code, NSString * _Nonnull message, NSArray<TXChorusUserInfo *> * _Nonnull userInfos) {
        @strongify(self)
        if (!self) {
            return;
        }
        TXChorusUserInfo *userInfo = [[TXChorusUserInfo alloc] init];
        if (code == 0 && userInfos.count > 0) {
            userInfo = userInfos[0];
        } else {
            TRTCLog(@"onSeat Take get user info error!");
            userInfo.userId = userId;
        }
        if ([self canDelegateResponseMethod:@selector(onSeatTakeWithIndex:userInfo:)]) {
            [self.delegate onSeatTakeWithIndex:index userInfo:userInfo];
        }
    }];
}

- (void)onSeatLeaveWithIndex:(NSInteger)index user:(NSString *)userId {
    TRTCLog(@"onSeatLeave: %ld, user: %@", (long)index, userId);
    @weakify(self)
    [self getUserInfo:@[userId] callback:^(int code, NSString * _Nonnull message, NSArray<TXChorusUserInfo *> * _Nonnull userInfos) {
        @strongify(self)
        if (!self) {
            return;
        }
        TXChorusUserInfo *userInfo = [[TXChorusUserInfo alloc] init];
        if (code == 0 && userInfos.count > 0) {
            userInfo = userInfos[0];
        } else {
            TRTCLog(@"onSeat Take get user info error!");
            userInfo.userId = userId;
        }
        if ([self canDelegateResponseMethod:@selector(onSeatLeaveWithIndex:userInfo:)]) {
            [self.delegate onSeatLeaveWithIndex:index userInfo:userInfo];
        }
    }];
}

- (void)onSeatcloseWithIndex:(NSInteger)index isClose:(BOOL)isClose {
    TRTCLog(@"onSeatClose: %ld", (long)index);
    if ([self canDelegateResponseMethod:@selector(onSeatCloseWithIndex:isClose:)]) {
        [self.delegate onSeatCloseWithIndex:index isClose:isClose];
    }
}


@end
