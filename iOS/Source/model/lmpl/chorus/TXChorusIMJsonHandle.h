//
//  TXChorusIMJsonHandle.h
//  TUIChorus
//
//  Created by adams on 2021/7/15.
//  Copyright © 2022 Tencent. All rights reserved.

#import <Foundation/Foundation.h>
#import "TXChorusBaseDef.h"

NS_ASSUME_NONNULL_BEGIN

static NSString* CHORUS_KEY_VERSION = @"version";
static NSString* CHORUS_KEY_BUSINESS_ID = @"businessID";
static NSString* CHORUS_KEY_PLATFORM = @"platform";
static NSString* CHORUS_KEY_EXTRAINFO = @"extInfo";
static NSString* CHORUS_KEY_ROOM_INFO = @"roomInfo";
static NSString* CHORUS_KEY_SEAT = @"seat";
static NSString* CHORUS_KEY_MESSAGE = @"message";
static NSString* CHORUS_KEY_CMD = @"cmd";
static NSString* CHORUS_KEY_DATA = @"data";
static NSString* CHORUS_KEY_ROOM_ID = @"room_id";
static NSString* CHORUS_KEY_ACTION = @"action";
static NSString* CHORUS_KEY_SEATNUMBER = @"seat_number";

static NSString* CHORUS_KEY_INVITATION_CMD = @"command";
static NSString* CHORUS_KEY_INVITAITON_CONTENT = @"content";
static NSString* CHORUS_KEY_INSTRUCTION = @"instruction";

static NSInteger CHORUS_VALUE_BASIC_VERSION = 1;
static NSInteger CHORUS_VALUE_VERSION = 1;
static NSString* CHORUS_VALUE_VERSION_STRING = @"1.0";
static NSString* CHORUS_VALUE_BUSINESS_ID = @"Chorus";
static NSString* CHORUS_VALUE_PLATFORM = @"iOS";

static NSString* CHORUS_VALUE_CMD_TAKESEAT = @"takeSeat";
static NSString* CHORUS_VALUE_CMD_PICKSEAT = @"pickSeat";

static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MPREPARE = @"m_prepare";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MCOMPLETE = @"m_complete";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MPLAYMUSIC = @"m_play_music";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MSTOP = @"m_stop";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MLISTCHANGE = @"m_list_change";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MPICK = @"m_pick";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MDELETE = @"m_delete";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MTOP = @"m_top";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MNEXT = @"m_next";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MGETLIST = @"m_get_list";
static NSString* CHORUS_VALUE_CMD_INSTRUCTION_MDELETEALL = @"m_delete_all";

typedef NS_ENUM(NSUInteger, TXChorusCustomCodeType) {
    kChorusCodeUnknown = 0,
    kChorusCodeDestroy = 200,
    kChorusCodeCustomMsg = 301,
};

@interface TXChorusIMJsonHandle : NSObject
+ (NSDictionary<NSString *, NSString *> *)getInitRoomDicWithRoomInfo:(TXChorusRoomInfo *)roominfo seatInfoList:(NSArray<TXChorusSeatInfo *> *)seatInfoList;
+ (NSString *)getRoomdestroyMsg;
+ (TXChorusRoomInfo * _Nullable)getRoomInfoFromAttr:(NSDictionary<NSString *, NSString *> *)attr;
+ (NSArray<TXChorusSeatInfo *> * _Nullable)getSeatListFromAttr:(NSDictionary<NSString *, NSString *> *)attr seatSize:(NSUInteger)seatSize;
+ (NSDictionary<NSString *, NSString *> *)getSeatInfoJsonStrWithIndex:(NSInteger)index info:(TXChorusSeatInfo *)info;
+ (NSString *)getCusMsgJsonStrWithCmd:(NSString *)cmd msg:(NSString *)msg;
+ (NSDictionary<NSString *, NSString *> *)parseCusMsgWithJsonDic:(NSDictionary *)jsonDic;
@end

NS_ASSUME_NONNULL_END
