//
//  TXChorusIMJsonHandle.h
//  TUIChorus
//
//  Created by adams on 2021/7/15.
//  Copyright © 2022 Tencent. All rights reserved.

#import <Foundation/Foundation.h>
#import "TXChorusBaseDef.h"

NS_ASSUME_NONNULL_BEGIN

static NSString* gCHORUS_KEY_VERSION = @"version";
static NSString* gCHORUS_KEY_BUSINESS_ID = @"businessID";
static NSString* gCHORUS_KEY_PLATFORM = @"platform";
static NSString* gCHORUS_KEY_EXTRAINFO = @"extInfo";
static NSString* gCHORUS_KEY_ROOM_INFO = @"roomInfo";
static NSString* gCHORUS_KEY_SEAT = @"seat";
static NSString* gCHORUS_KEY_MESSAGE = @"message";
static NSString* gCHORUS_KEY_CMD = @"cmd";
static NSString* gCHORUS_KEY_DATA = @"data";
static NSString* gCHORUS_KEY_ROOM_ID = @"room_id";
static NSString* gCHORUS_KEY_ACTION = @"action";
static NSString* gCHORUS_KEY_SEATNUMBER = @"seat_number";

static NSString* gCHORUS_KEY_INVITATION_CMD = @"command";
static NSString* gCHORUS_KEY_INVITAITON_CONTENT = @"content";
static NSString* gCHORUS_KEY_INSTRUCTION = @"instruction";

static NSInteger gCHORUS_VALUE_BASIC_VERSION = 1;
static NSInteger gCHORUS_VALUE_VERSION = 1;
static NSString* gCHORUS_VALUE_VERSION_STRING = @"1.0";
static NSString* gCHORUS_VALUE_BUSINESS_ID = @"Chorus";
static NSString* gCHORUS_VALUE_PLATFORM = @"iOS";

static NSString* gCHORUS_VALUE_CMD_TAKESEAT = @"takeSeat";
static NSString* gCHORUS_VALUE_CMD_PICKSEAT = @"pickSeat";

static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MPREPARE = @"m_prepare";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MCOMPLETE = @"m_complete";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MPLAYMUSIC = @"m_play_music";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MSTOP = @"m_stop";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MLISTCHANGE = @"m_list_change";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MPICK = @"m_pick";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MDELETE = @"m_delete";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MTOP = @"m_top";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MNEXT = @"m_next";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MGETLIST = @"m_get_list";
static NSString* gCHORUS_VALUE_CMD_INSTRUCTION_MDELETEALL = @"m_delete_all";

typedef NS_ENUM(NSUInteger, TXChorusCustomCodeType) {
    kChorusCodeUnknown = 0,
    kChorusCodeDestroy = 200,
    kChorusCodeCustomMsg = 301,
};

@interface TXChorusIMJsonHandle : NSObject
+ (NSDictionary<NSString *, NSString *> *)getInitRoomDicWithRoomInfo:(TXChorusRoomInfo *)roominfo
 seatInfoList:(NSArray<TXChorusSeatInfo *> *)seatInfoList;
+ (NSString *)getRoomdestroyMsg;
+ (TXChorusRoomInfo * _Nullable)getRoomInfoFromAttr:(NSDictionary<NSString *, NSString *> *)attr;
+ (NSArray<TXChorusSeatInfo *> * _Nullable)getSeatListFromAttr:(NSDictionary<NSString *, NSString *> *)attr seatSize:(NSUInteger)seatSize;
+ (NSDictionary<NSString *, NSString *> *)getSeatInfoJsonStrWithIndex:(NSInteger)index info:(TXChorusSeatInfo *)info;
+ (NSString *)getCusMsgJsonStrWithCmd:(NSString *)cmd msg:(NSString *)msg;
+ (NSDictionary<NSString *, NSString *> *)parseCusMsgWithJsonDic:(NSDictionary *)jsonDic;
@end

NS_ASSUME_NONNULL_END
