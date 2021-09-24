//
//  TRTCChorusRoomDef.h
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface ChorusSeatInfo : NSObject

@property (nonatomic, assign) NSInteger status;
@property (nonatomic, strong) NSString *userId;

@end

@interface ChorusParam : NSObject

@property (nonatomic, strong) NSString *rtmpPushURL;
@property (nonatomic, strong) NSString *rtmpPlayURL;
@property (nonatomic, strong) NSString *roomName;
@property (nonatomic, strong) NSString *coverUrl;
@property (nonatomic, assign) BOOL needRequest;
@property (nonatomic, assign) NSInteger seatCount;
@property (nonatomic, strong) NSArray<ChorusSeatInfo *> *seatInfoList;

@end

@interface ChorusUserInfo : NSObject

@property (nonatomic, strong) NSString *userId;
@property (nonatomic, strong) NSString *userName;
@property (nonatomic, strong) NSString *userAvatar;
@property (nonatomic, assign) BOOL mute;

@end

@interface ChorusRoomInfo : NSObject

@property (nonatomic, assign) NSInteger roomID;
@property (nonatomic, strong) NSString *roomName;
@property (nonatomic, strong) NSString *coverUrl;
@property (nonatomic, strong) NSString *rtmpPushURL;
@property (nonatomic, strong) NSString *rtmpPlayURL;
@property (nonatomic, strong) NSString *ownerId;
@property (nonatomic, strong) NSString *ownerName;
@property (nonatomic, assign) NSInteger memberCount;
@property (nonatomic, assign) BOOL needRequest;

-(instancetype)initWithRoomID:(NSInteger)roomID ownerId:(NSString *)ownerId memberCount:(NSInteger)memberCount;

@end

typedef void(^ActionCallback)(int code, NSString * _Nonnull message);
typedef void(^ChorusRoomInfoCallback)(int code, NSString * _Nonnull message, NSArray<ChorusRoomInfo * > * _Nonnull roomInfos);
typedef void(^ChorusUserListCallback)(int code, NSString * _Nonnull message, NSArray<ChorusUserInfo * > * _Nonnull userInfos);

NS_ASSUME_NONNULL_END
