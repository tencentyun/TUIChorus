//
//  TRTCChorusViewDataDef.swift
//  TUIChorus
//
//  Created by adams on 2021/7/15.
//  Copyright © 2022 Tencent. All rights reserved.

import UIKit

public enum ChorusRoleType {
    // 主播（房主）
    case anchor
    // 合唱者
    case chorus
    // 观众
    case audience
}

/// Voice Room 定义常量的类
class ChorusConstants {
    public static let TYPE_VOICE_ROOM = "Chorus"
    // 直播端右下角listview显示的type
    public static let CMD_REQUEST_TAKE_SEAT = "1"
    public static let CMD_PICK_UP_SEAT = "2"
}

/// 记录房间座位信息的Model
struct SeatInfoModel {
    var seatIndex: Int = -1
    var isClosed: Bool = false
    var isUsed: Bool = false
    var isOwner: Bool = false
    var seatInfo: ChorusSeatInfo?
    var seatUser: ChorusUserInfo?
    var action: ((Int) -> Void)? // 入参为SeatIndex
    var isTalking: Bool = false
}

struct AudienceInfoModel {
    
    static let TYPE_IDEL = 0
    static let TYPE_IN_SEAT = 1
    static let TYPE_WAIT_AGREE = 2
    
    var type: Int = 0 // 观众类型
    var userInfo: ChorusUserInfo
    var action: (Int) -> Void // 点击邀请按钮的动作
}

enum MsgEntityType: Int {
    case normal = 0
    case wait_agree
    case agreed
    case manage_song
}

/// 记录房间消息列表的Model
struct MsgEntity {
    let userId: String
    let userName: String
    let content: String
    let invitedId: String
    var type: MsgEntityType
    var action: (()->())? = nil
}

struct SeatInvitation {
    let seatIndex: Int
    let inviteUserId: String
}
