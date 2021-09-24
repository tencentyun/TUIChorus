//
//  TXChorusBaseDef.m
//  TUIChorus
//
//  Created by adams on 2020/7/14.
//

#import "TXChorusBaseDef.h"

@implementation TXChorusRoomInfo

// 默认值与业务逻辑统一
- (instancetype)init {
    if (self = [super init]) {
        self.needRequest = YES;
    }
    return self;
}

@end

@implementation TXChorusUserInfo


@end

@implementation TXChorusSeatInfo

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.status = 0;
        self.user = @"";
    }
    return self;
}

@end

@implementation TXChorusInviteData


@end
