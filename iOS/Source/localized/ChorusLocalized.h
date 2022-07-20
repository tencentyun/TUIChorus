//
//  ChorusLocalized.h
//  Pods
//
//  Created by adams on 2021/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN


#pragma mark - Base
extern NSBundle *ChorusBundle(void);
extern NSString *ChorusLocalizeFromTable(NSString *key, NSString *table);
extern NSString *ChorusLocalizeFromTableAndCommon(NSString *key, NSString *common, NSString *table);

#pragma mark - Replace String
extern NSString *LocalizeReplaceXX(NSString *origin, NSString *xxx_replace);
extern NSString *LocalizeReplace(NSString *origin, NSString *xxx_replace, NSString *yyy_replace);
extern NSString *LocalizeReplaceThreeCharacter(NSString *origin, NSString *xxx_replace, NSString *yyy_replace, NSString *zzz_replace);
extern NSString *LocalizeReplaceFourCharacter(NSString *origin, NSString *xxx_replace, NSString *yyy_replace, NSString *zzz_replace, NSString *mmm_replace);
extern NSString *LocalizeReplaceFiveCharacter(NSString *origin, NSString *xxx_replace, NSString *yyy_replace, NSString *zzz_replace, NSString *mmm_replace, NSString *nnn_replace);

#pragma mark - Chorus
extern NSString *const Chorus_Localize_TableName;
extern NSString *ChorusLocalize(NSString *key);

NS_ASSUME_NONNULL_END
