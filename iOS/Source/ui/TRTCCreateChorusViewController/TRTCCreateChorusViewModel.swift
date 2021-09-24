//
//  TRTCCreateChorusViewModel.swift
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//

import UIKit
import ImSDK_Plus
import TXAppBasic

public enum ChorusToneQuality: Int {
    case speech = 1
    case defaultQuality
    case music
}

public protocol TRTCCreateChorusViewResponder: NSObject {
    func push(viewController: UIViewController)
    func pop()
}

public class TRTCCreateChorusViewModel {
    private let dependencyContainer: TRTCChorusEnteryControl
    public weak var viewResponder: TRTCCreateChorusViewResponder?
    public weak var musicDataSource: ChorusMusicService?
    
    var chorusRoom: TRTCChorusRoom {
        return dependencyContainer.getChorusRoom()
    }
    
    var roomName: String = ""
    var userName: String {
        get {
            return TRTCChorusIMManager.sharedManager().curUserName
        }
    }
    var userID: String {
        return V2TIMManager.sharedInstance()?.getLoginUser() ?? ""
    }
    var needRequest: Bool = true
    var toneQuality: ChorusToneQuality = .defaultQuality
    
    /// 初始化方法
    /// - Parameter container: 依赖管理容器，负责Chorus模块的依赖管理
    init(dependencyContainer: TRTCChorusEnteryControl) {
        self.dependencyContainer = dependencyContainer
    }
    
    public func navigationPop() {
        if let viewResponder = viewResponder {
            viewResponder.pop()
        }
    }
    
    private func randomBgImageLink() -> String {
        let random = arc4random() % 12 + 1
        return "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover\(random).png"
    }
    
    private func getRoomId() -> Int {
        let userId = userID
        let result = "\(userId)_voice_room".hash & 0x7FFFFFFF
        TRTCLog.out("hashValue:room id:\(result), userId: \(userId)")
        return result
    }
    
    func createRoom() {
        guard let musicDataSource = musicDataSource else {
            return
        }
        let userId = userID
        let coverAvatar = randomBgImageLink()
        let roomId = getRoomId()
        let roomInfo = ChorusRoomInfo.init(roomID: roomId, ownerId: userId, memberCount: 2)
        dependencyContainer.generateRTMPPushURL { [weak self] (info) in
            guard let `self` = self else { return }
            if let info = info {
                roomInfo.rtmpPushURL = info.0
                roomInfo.rtmpPlayURL = info.1
                roomInfo.ownerName = self.userName
                roomInfo.coverUrl = coverAvatar
                roomInfo.roomName = self.roomName
                roomInfo.needRequest = self.needRequest
                let chorusVC = self.dependencyContainer.makeChorusViewController(roomInfo: roomInfo, roleType: .anchor, toneQuality: self.toneQuality, musicDataSource: musicDataSource)
                if let viewResponder = self.viewResponder {
                    viewResponder.push(viewController: chorusVC)
                }
            }
        }
    }
}
