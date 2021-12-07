//
//  TXChorusBaseDef.h
//  TUIChorus
//
//  Created by adams on 2020/7/14.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT void TUIChorusLog(NSString *format, ...);

// 使用TRTCCloud apiLog，日志会写入本地
#define TRTCLog(fmt, ...) TUIChorusLog((@"TRTC LOG:%s [Line %d] " fmt), __PRETTY_FUNCTION__, __LINE__, ##__VA_ARGS__)

@class TXChorusUserInfo;
@class TXChorusRoomInfo;

typedef void(^TXChorusCallback)(int code, NSString *message);
typedef void(^TXChorusUserListCallback)(int code, NSString *message, NSArray<TXChorusUserInfo *> *userInfos);
typedef void(^TXChorusRoomInfoListCallback)(int code, NSString *message, NSArray<TXChorusRoomInfo *> *roomInfos);

typedef NS_ENUM(NSUInteger, TXChorusSeatStatus) {
    kTXChorusSeatStatusUnused = 0,
    kTXChorusSeatStatusUsed = 1,
    kTXChorusSeatStatusClosed = 2,
};

@interface TXChorusRoomInfo : NSObject

@property (nonatomic, strong) NSString *roomId;
@property (nonatomic, assign) UInt32 memberCount;
@property (nonatomic, strong) NSString *playUrl;

@property (nonatomic, strong) NSString *ownerId;
@property (nonatomic, strong) NSString *ownerName;
@property (nonatomic, strong) NSString *roomName;
@property (nonatomic, strong) NSString *cover;
@property (nonatomic, assign) NSInteger seatSize;
@property (nonatomic, assign) NSInteger needRequest;

@end

@interface TXChorusUserInfo : NSObject

@property (nonatomic, strong) NSString *userId;
@property (nonatomic, strong) NSString *userName;
@property (nonatomic, strong) NSString *avatarURL;

@end

@interface TXChorusSeatInfo : NSObject

@property (nonatomic, assign) NSInteger status;
@property (nonatomic, strong) NSString *user;

@end

@interface TXChorusInviteData : NSObject

@property (nonatomic, strong) NSString *roomId;
@property (nonatomic, strong) NSString *command;
@property (nonatomic, strong) NSString *message;

@end

NS_ASSUME_NONNULL_END
