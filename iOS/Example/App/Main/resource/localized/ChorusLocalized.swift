//
//  ChorusLocalized.swift
//  TRTCAPP_AppStore
//
//  Created by adams on 2021/6/4.
//

import Foundation

let ChorusLocalizeTableName = "ChorusLocalized"
func TRTCChorusLocalize(_ key: String) -> String {
    return localizeFromTable(key: key, table: ChorusLocalizeTableName)
}
