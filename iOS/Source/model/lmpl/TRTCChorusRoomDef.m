//
//  TRTCChorusRoomDef.m
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

#import "TRTCChorusRoomDef.h"

@implementation ChorusParam

@end

@implementation ChorusSeatInfo

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.status = 0;
        self.userId = @"";
    }
    return self;
}

@end

@implementation ChorusUserInfo

- (instancetype)init {
    if (self = [super init]) {
        self.mute = YES;
    }
    return self;
}

- (void)setUserName:(NSString *)userName{
    if (!userName) {
        userName = @"";
    }
    _userName = userName;
}

- (void)setUserAvatar:(NSString *)userAvatar{
    if (!userAvatar) {
        userAvatar = @"";
    }
    _userAvatar = userAvatar;
}

@end

@implementation ChorusRoomInfo

-(instancetype)initWithRoomID:(NSInteger)roomID ownerId:(NSString *)ownerId memberCount:(NSInteger)memberCount {
    self = [super init];
    if (self) {
        self.roomID = roomID;
        self.ownerId = ownerId;
        self.memberCount = memberCount;
    }
    return self;
}

@end
