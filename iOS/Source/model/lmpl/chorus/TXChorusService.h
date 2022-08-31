//
//  TXChorusService.h
//  Alamofire
//
//  Created by adams on 2021/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

#import <Foundation/Foundation.h>
#import "TXChorusBaseDef.h"

NS_ASSUME_NONNULL_BEGIN

@protocol TXChorusRoomServiceDelegate <NSObject>
- (void)onRoomDestroyWithRoomId:(NSString *)roomID;
- (void)onRoomInfoChange:(TXChorusRoomInfo *)roomInfo;
- (void)onSeatInfoListChange:(NSArray<TXChorusSeatInfo *> *)seatInfoList;
- (void)onRoomRecvRoomTextMsg:(NSString *)roomID message:(NSString *)message userInfo:(TXChorusUserInfo *)userInfo;
- (void)onRoomRecvRoomCustomMsg:(NSString *)roomID cmd:(NSString *)cmd message:(NSString *)message userInfo:(TXChorusUserInfo *)userInfo;
- (void)onRoomAudienceEnter:(TXChorusUserInfo *)userInfo;
- (void)onRoomAudienceLeave:(TXChorusUserInfo *)userInfo;
- (void)onSeatTakeWithIndex:(NSInteger)index userInfo:(TXChorusUserInfo *)userInfo;
- (void)onSeatLeaveWithIndex:(NSInteger)index userInfo:(TXChorusUserInfo *)userInfo;
- (void)onSeatCloseWithIndex:(NSInteger)index isClose:(BOOL)isClose;
- (void)onReceiveNewInvitationWithIdentifier:(NSString *)identifier inviter:(NSString *)inviter cmd:(NSString *)cmd content:(NSString *)content;
- (void)onInviteeAcceptedWithIdentifier:(NSString *)identifier invitee:(NSString *)invitee;
- (void)onInviteeRejectedWithIdentifier:(NSString *)identifier invitee:(NSString *)invitee;
- (void)onInviteeCancelledWithIdentifier:(NSString *)identifier invitee:(NSString *)invitee;
@end

static int gCHORUS_SERVICE_CODE_ERROR = -1;

@interface TXChorusService : NSObject
@property (nonatomic, weak) id<TXChorusRoomServiceDelegate> delegate;
@property (nonatomic, assign, readonly) BOOL isOwner;
@property (nonatomic, strong, readonly) TXChorusRoomInfo *roomInfo;

+ (instancetype)sharedInstance;
- (void)loginWithSdkAppId:(int)sdkAppId userId:(NSString *)userId userSig:(NSString *)userSig callback:(TXChorusCallback _Nullable)callback;
- (void)logout:(TXChorusCallback _Nullable)callback;
- (void)getSelfInfo;
- (void)setSelfProfileWithUserName:(NSString *)userName
                         avatarUrl:(NSString *)avatarUrl
                          callback:(TXChorusCallback _Nullable)callback;

- (void)createRoomWithRoomId:(NSString *)roomId
                    roomName:(NSString *)roomName
                    coverUrl:(NSString *)coverUrl
                     playUrl:(NSString *)playUrl
                 needRequest:(BOOL)needRequest
                seatInfoList:(NSArray<TXChorusSeatInfo *> *)seatInfoList
                    callback:(TXChorusCallback _Nullable)callback;

- (void)destroyRoom:(TXChorusCallback _Nullable)callback;
- (void)enterRoom:(NSString *)roomId callback:(TXChorusCallback _Nullable)callback;
- (void)exitRoom:(TXChorusCallback _Nullable)callback;
- (void)takeSeat:(NSInteger)seatIndex callback:(TXChorusCallback _Nullable)callback;
- (void)leaveSeat:(NSInteger)seatIndex callback:(TXChorusCallback _Nullable)callback;
- (void)pickSeat:(NSInteger)seatIndex userId:(NSString *)userId callback:(TXChorusCallback _Nullable)callback;
- (void)kickSeat:(NSInteger)seatIndex callback:(TXChorusCallback _Nullable)callback;
- (void)closeSeat:(NSInteger)seatIndex isClose:(BOOL)isClose callback:(TXChorusCallback _Nullable)callback;
- (void)getRoomInfoList:(NSArray<NSString *> *)roomIds calback:(TXChorusRoomInfoListCallback _Nullable)callback;
- (void)getUserInfo:(NSArray<NSString *> *)userList callback:(TXChorusUserListCallback _Nullable)callback;
- (void)getAudienceList:(TXChorusUserListCallback _Nullable)callback;
- (void)destroy;
- (void)sendRoomTextMsg:(NSString *)msg callback:(TXChorusCallback _Nullable)callback;
- (void)sendRoomCustomMsg:(NSString *)cmd message:(NSString *)message callback:(TXChorusCallback _Nullable)callback;
- (NSString *)sendInvitation:(NSString *)cmd userId:(NSString *)userId content:(NSString *)content callback:(TXChorusCallback _Nullable)callback;
- (void)acceptInvitation:(NSString *)identifier callback:(TXChorusCallback _Nullable)callback;
- (void)rejectInvitaiton:(NSString *)identifier callback:(TXChorusCallback _Nullable)callback;
- (void)cancelInvitation:(NSString *)identifier callback:(TXChorusCallback _Nullable)callback;
@end

NS_ASSUME_NONNULL_END
