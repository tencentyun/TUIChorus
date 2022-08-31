//
//  ChorusLocalized.m
//  Pods
//
//  Created by adams on 2021/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

#import "ChorusLocalized.h"

#pragma mark - Base

NSBundle *chorusBundle(void) {
    NSURL *callingKitBundleURL = [[NSBundle mainBundle] URLForResource:@"TUIChorusKitBundle" withExtension:@"bundle"];
    return [NSBundle bundleWithURL:callingKitBundleURL];
}

NSString *chorusLocalizeFromTable(NSString *key, NSString *table) {
    return [chorusBundle() localizedStringForKey:key value:@"" table:table];
}

NSString *chorusLocalizeFromTableAndCommon(NSString *key, NSString *common, NSString *table) {
    return chorusLocalizeFromTable(key, table);
}

#pragma mark - Replace String
NSString *localizeReplaceXX(NSString *origin, NSString *xxx_replace) {
    if (xxx_replace == nil) { xxx_replace = @"";}
    return [origin stringByReplacingOccurrencesOfString:@"xxx" withString:xxx_replace];
}

NSString *localizeReplace(NSString *origin, NSString *xxx_replace, NSString *yyy_replace) {
    if (yyy_replace == nil) { yyy_replace = @"";}
    return [localizeReplaceXX(origin, xxx_replace) stringByReplacingOccurrencesOfString:@"yyy" withString:yyy_replace];
}

NSString *localizeReplaceThreeCharacter(NSString *origin, NSString *xxx_replace, NSString
 *yyy_replace, NSString *zzz_replace) {
    if (zzz_replace == nil) { zzz_replace = @"";}
    return [localizeReplace(origin, xxx_replace, yyy_replace) stringByReplacingOccurrencesOfString:@"zzz" withString:zzz_replace];
}

NSString *localizeReplaceFourCharacter(NSString *origin, NSString *xxx_replace, NSString *yyy_replace, NSString *zzz_replace, NSString *mmm_replace) {
    if (mmm_replace == nil) { mmm_replace = @"";}
    return [localizeReplaceThreeCharacter(origin, xxx_replace, yyy_replace, zzz_replace)
     stringByReplacingOccurrencesOfString:@"mmm" withString:mmm_replace];
}

NSString *localizeReplaceFiveCharacter(NSString *origin, NSString *xxx_replace, NSString
 *yyy_replace, NSString *zzz_replace, NSString *mmm_replace, NSString *nnn_replace) {
    if (nnn_replace == nil) { nnn_replace = @"";}
    return [localizeReplaceFourCharacter(origin, xxx_replace, yyy_replace, zzz_replace,
    mmm_replace) stringByReplacingOccurrencesOfString:@"nnn" withString:nnn_replace];
}


#pragma mark - Chorus
NSString *const chorus_Localize_TableName = @"ChorusLocalized";
NSString *chorusLocalize(NSString *key) {
    return chorusLocalizeFromTable(key, chorus_Localize_TableName);
}
