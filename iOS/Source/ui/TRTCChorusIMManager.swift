//
//  TRTCChorusIMManager.swift
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//

import Foundation
import ImSDK_Plus

public class TRTCChorusIMManager: NSObject {

    private static let staticInstance: TRTCChorusIMManager = TRTCChorusIMManager.init()
    public static func sharedManager() -> TRTCChorusIMManager { staticInstance }
    private override init(){}
    
    public var curUserID: String = ""
    public var curUserName: String = ""
    public var curUserAvatar: String = ""
    public var SDKAPPID: Int32 = 0
    public var seatIndex: Int = -1
    
    public func loadData() {
        curUserID = V2TIMManager.sharedInstance()?.getLoginUser() ?? ""
        V2TIMManager.sharedInstance()?.getUsersInfo([curUserID], succ: { [weak self] (infos) in
            guard let `self` = self else { return }
            guard let info = infos?.first else {
                return
            }
            self.curUserName = info.nickName
            self.curUserAvatar = info.faceURL
        }, fail: { (code, msg) in
            
        })
    }
    
    
    public func checkAvatar(_ avatar: String) -> String {
        if avatar == "avatar" || avatar.count == 0 {
             return "https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar1.png"
        }
        return avatar
    }
    
    public func checkBgImage(_ image: String) -> String {
        if image == "avatar" || image.count == 0 {
             return "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover1.png"
        }
        return image
    }
    
    public func loginAndSetSelfProfile(sdkAppID: Int32, userId: String, userSig: String, userName: String, avatarURL: String, _ completion: @escaping ActionCallback) {
        var resCode: Int32 = 0
        var resMsg: String = ""
        let avatar = checkAvatar(avatarURL)
        loginChorus(sdkAppID: sdkAppID, userId: userId, userSig: userSig) { [weak self] (code, msg) in
            guard let `self` = self else { return }
            resCode += code
            resMsg = msg
            debugPrint("Chorus: login \(code == 0 ? "success" : "failed")")
            self.setSelfProfile(userName: userName, avatarURL: avatar) { [weak self] (code, msg) in
                guard let `self` = self else { return }
                resCode += code
                if resMsg.count == 0 {
                    resMsg = msg
                }
                debugPrint("Chorus: set self profile \(code == 0 ? "success" : "failed")")
                if resCode == 0 {
                    self.curUserID = userId
                    self.curUserName = userName
                    self.curUserAvatar = avatarURL
                }
                completion(resCode, resMsg)
            }
        }
    }
    
    public func loginChorus(sdkAppID: Int32, userId: String, userSig: String, _ completion: @escaping ActionCallback) {
        SDKAPPID = sdkAppID
        curUserID = userId
        TRTCChorusRoom.shared().login(sdkAppID: sdkAppID, userId: userId, userSig: userSig, callback: completion)
    }
    
    public func setSelfProfile(userName: String, avatarURL: String, _ completion: @escaping ActionCallback) {
        TRTCChorusRoom.shared().setSelfProfile(userName: userName, avatarURL: avatarURL, callback: completion)
    }
    
    public func logoutChorus() {
        TRTCChorusRoom.shared().logout { [weak self] (code, msg) in
            guard let `self` = self else { return }
            self.curUserID = ""
            self.curUserName = ""
            self.curUserAvatar = ""
        }
    }
}

