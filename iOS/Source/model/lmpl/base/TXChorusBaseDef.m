//
//  TXChorusBaseDef.m
//  TUIChorus
//
//  Created by adams on 2020/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

#import "TXChorusBaseDef.h"
#import "TRTCCloud.h"

@interface TRTCCloud (ChorusLog)

// 打印一些关键log到本地日志中
- (void)apiLog:(NSString *)log;

@end

void TUIChorusLog(NSString *format, ...){
    if (!format || ![format isKindOfClass:[NSString class]] || format.length == 0) {
        return;
    }
    va_list arguments;
    va_start(arguments, format);
    NSString *content = [[NSString alloc] initWithFormat:format arguments:arguments];
    va_end(arguments);
    if (content) {
        [[TRTCCloud sharedInstance] apiLog:content];
    }
}

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
