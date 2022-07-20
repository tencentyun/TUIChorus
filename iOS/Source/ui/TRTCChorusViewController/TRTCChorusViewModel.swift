//
//  TRTCChorusViewModel.swift
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

import UIKit
import TXAppBasic

protocol TRTCChorusViewNavigator: NSObject {
    func popToPrevious()
    func presentAlert(viewController: UIViewController, animated: Bool, completion: (() -> Void)?)
}

protocol TRTCChorusViewResponder: NSObject {
    func showToast(message: String)
    func showToastActivity()
    func hiddenToastActivity()
    func switchView(type: ChorusRoleType)
    func changeRoom(info: ChorusRoomInfo)
    func refreshAnchorInfos()
    func onSeatMute(isMute: Bool)
    func onAnchorMute(isMute: Bool)
    func showAlert(info: (title: String, message: String), sureAction: @escaping () -> Void, cancelAction: (() -> Void)?)
    func showActionSheet(actionTitles:[String], actions: @escaping (Int) -> Void)
    func refreshMsgView()
    func msgInput(show: Bool)
    func audiceneList(show: Bool)
    func audienceListRefresh()
    func stopPlayBGM() // 停止播放音乐
    func recoveryVoiceSetting() // 恢复音效设置
    func showAudienceAlert(seat: SeatInfoModel)
    func showGiftAnimation(giftInfo: TUIGiftInfo)
    func updateLocalNetwork(network: Int)
    func updateRemoteNetwork(network: Int)
    func onShowChorusGifAnimation()
    func onHiddenChorusGifAnimation()
}

class TRTCChorusViewModel: NSObject {
    private let kSendGiftCmd = "0"
    private let dependencyContainer: TRTCChorusEnteryControl
    private let giftManager = TUIGiftManager.sharedManager()
    private var roleType: ChorusRoleType = .audience
    // 防止多次退房
    private var isExitingRoom: Bool = false
   
    // 当前麦上用户
    private var currentSeatUserInfo: ChorusUserInfo? = nil
    public var currentMusicModel: ChorusMusicModel? = nil
    
    public weak var rootVC: TRTCChorusViewController?
    public weak var viewResponder: TRTCChorusViewResponder?
    public weak var viewNavigator: TRTCChorusViewNavigator?
    public weak var musicDataSource: ChorusMusicService? {
        didSet {
            musicDataSource?.setRoomInfo(roomInfo: roomInfo)
            musicDataSource?.setServiceDelegate(self)
        }
    }
    
    public var chorusAnchorList: [SeatInfoModel] = []
    public var muteItem: IconTuple?
    public var userType: ChorusRoleType = .audience
    public var userMuteMap : [String : Bool] = [:]
    public var isOwner: Bool {
        return TRTCChorusIMManager.sharedManager().curUserID == roomInfo.ownerId
    }
    public var voiceEarMonitor: Bool = false {
        willSet {
            self.chorusRoom.setVoiceEarMonitor(enable: newValue)
        }
    }
    
    
    private(set) var msgEntityList: [MsgEntity] = []
    /// 当前邀请操作的座位号记录
    private var currentInvitateSeatIndex: Int = -1 // -1 表示没有操作
    /// 上麦信息记录(观众端)
    private var mInvitationSeatDic: [String: Int] = [:]
    /// 上麦信息记录(主播端)
    private var mTakeSeatInvitationDic: [String: String] = [:]
    /// 抱麦信息记录
    private var mPickSeatInvitationDic: [String: SeatInvitation] = [:]
    
    /// 观众信息记录
    private(set) var memberAudienceList: [AudienceInfoModel] = []
    private(set) var memberAudienceDic: [String: AudienceInfoModel] = [:]
    private(set) var mSelfSeatIndex: Int = -1
    private(set) var anchorSeatList: [SeatInfoModel] = []
    private(set) var roomInfo: ChorusRoomInfo
    private(set) var isSeatInitSuccess: Bool = false
    private(set) var isOwnerMute: Bool = false
    private(set) var isSelfMute: Bool = false
   
    /// 初始化方法
    /// - Parameter container: 依赖管理容器，负责Chorus模块的依赖管理
    init(container: TRTCChorusEnteryControl, roomInfo: ChorusRoomInfo, roleType: ChorusRoleType) {
        self.dependencyContainer = container
        self.roleType = roleType
        self.roomInfo = roomInfo
        super.init()
        chorusRoom.setDelegate(delegate: self)
        initAnchorListData()
    }
    
    deinit {
        TRTCLog.out("deinit \(type(of: self))")
    }
    
    public var chorusRoom: TRTCChorusRoom {
        return dependencyContainer.getChorusRoom()
    }
    
    lazy var effectViewModel: TRTCChorusSoundEffectViewModel = {
        return TRTCChorusSoundEffectViewModel(self)
    }()
}

//MARK: - private method
extension TRTCChorusViewModel {
    
    private func initAnchorListData() {
        for _ in 0...1 {
            var model = SeatInfoModel.init { [weak self] (seatIndex) in
                guard let `self` = self else { return }
                if seatIndex >= 0 && seatIndex <= self.anchorSeatList.count {
                    let model = self.anchorSeatList[seatIndex]
                    print("=====\(model.seatIndex)")
                    self.clickSeat(model: model)
                }
            }
            model.isOwner = TRTCChorusIMManager.sharedManager().curUserID == roomInfo.ownerId
            model.isClosed = false
            model.isUsed = false
            anchorSeatList.append(model)
        }
    }
    
    private func internalCreateRoom() {
        let param = ChorusParam.init()
        param.roomName = roomInfo.roomName
        param.needRequest = roomInfo.needRequest
        param.seatCount = roomInfo.memberCount
        param.coverUrl = roomInfo.coverUrl
        param.seatCount = 2
        param.seatInfoList = []
        param.rtmpPushURL = roomInfo.rtmpPushURL
        param.rtmpPlayURL = roomInfo.rtmpPlayURL
        for _ in 0..<param.seatCount {
            let seatInfo = ChorusSeatInfo.init()
            param.seatInfoList.append(seatInfo)
        }
        chorusRoom.createRoom(roomID: Int32(roomInfo.roomID), roomParam: param) { [weak self] (code, message) in
            guard let `self` = self else { return }
            if code == 0 {
                self.viewResponder?.changeRoom(info: self.roomInfo)
                self.getAudienceList()
                if self.isOwner {
                    self.startTakeSeat(seatIndex: 0)
                }
            } else {
                self.viewResponder?.showToast(message: .enterFailedText)
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
                    guard let `self` = self else { return }
                    self.viewNavigator?.popToPrevious()
                }
            }
        }
    }
    
    private func getAudienceList() {
        chorusRoom.getUserInfoList(userIDList: nil) { [weak self] (code, message, infos) in
            guard let `self` = self else { return }
            if code == 0 {
                self.memberAudienceList.removeAll()
                let audienceInfoModels = infos.map { (userInfo) -> AudienceInfoModel in
                    return AudienceInfoModel.init(userInfo: userInfo) { [weak self] index in
                        guard let `self` = self else { return }
                        // 点击邀请上麦事件，以及接受邀请事件
                        if index == 0 {
                            self.sendInvitation(userInfo: userInfo)
                        } else {
                            self.acceptTakeSeatInviation(userInfo: userInfo)
                        }
                    }
                }
                self.memberAudienceList.append(contentsOf: audienceInfoModels)
                self.viewResponder?.audienceListRefresh()
            }
        }
    }
    
    // 邀请上麦
    private func sendInvitation(userInfo: ChorusUserInfo) {
        guard currentInvitateSeatIndex != -1 else { return }
        // 邀请
        let seatEntity = anchorSeatList[currentInvitateSeatIndex]
        if seatEntity.isUsed {
            viewResponder?.showToast(message: .seatBusyText)
            return
        }
        let seatInvitation = SeatInvitation.init(seatIndex: currentInvitateSeatIndex, inviteUserId: userInfo.userId)
        let inviteId = chorusRoom.sendInvitation(cmd: ChorusConstants.CMD_PICK_UP_SEAT,
                                                userId: seatInvitation.inviteUserId,
                                                content: "\(seatInvitation.seatIndex)") { [weak self] (code, message) in
                                                    guard let `self` = self else { return }
                                                    if code == 0 {
                                                        self.viewResponder?.showToast(message: .sendInviteSuccessText)
                                                    }
        }
        mPickSeatInvitationDic[inviteId] = seatInvitation
        viewResponder?.audiceneList(show: false)
    }
    
    private func acceptTakeSeatInviation(userInfo: ChorusUserInfo) {
        // 接受
        guard let inviteID = mTakeSeatInvitationDic[userInfo.userId] else {
            viewResponder?.showToast(message: .reqExpiredText)
            return
        }
        chorusRoom.acceptInvitation(identifier: inviteID) { [weak self] (code, message) in
            guard let `self` = self else { return }
            if code == 0 {
                // 接受请求成功，刷新外部对话列表
                if let index = self.msgEntityList.firstIndex(where: { (msg) -> Bool in
                    return msg.invitedId == inviteID
                }) {
                    var msg = self.msgEntityList[index]
                    msg.type = .agreed
                    self.msgEntityList[index] = msg
                    self.viewResponder?.refreshMsgView()
                }
            } else {
                self.viewResponder?.showToast(message: .acceptReqFailedText)
            }
        }
    }
    
    private func audienceClickItem(model: SeatInfoModel) {
        guard !model.isClosed else {
            viewResponder?.showToast(message: .seatLockedText)
            return
        }
        if model.isUsed {
            if TRTCChorusIMManager.sharedManager().curUserID == model.seatUser?.userId ?? "" {
                viewResponder?.showAlert(info: (title: .sureToLeaveSeatText, message: ""), sureAction: { [weak self] in
                    guard let `self` = self else { return }
                    self.leaveSeat()
                }, cancelAction: {
                    
                })
            } else {
                viewResponder?.showToast(message: "\(model.seatUser?.userName ?? .otherAnchorText)")
            }
        }
        else {
            if mSelfSeatIndex != -1 {
                viewResponder?.showToast(message: LocalizeReplaceXX(.isInxxSeatText, String(mSelfSeatIndex + 1)))
                return
            }
            guard model.seatIndex != -1 else {
                viewResponder?.showToast(message: .notInitText)
                return
            }
            viewResponder?.showActionSheet(actionTitles: [.handsupText], actions: { [weak self] (index) in
                guard let `self` = self else { return }
                self.startTakeSeat(seatIndex: model.seatIndex)
            })
        }
    }
    
    private func ownerClickItem(model: SeatInfoModel) {
        if (model.seatIndex == 0) {
            // 合唱场景 主播不下麦
            return
        }
        if model.isUsed {
            if model.seatUser?.userId == TRTCChorusIMManager.sharedManager().curUserID {
                viewResponder?.showAlert(info: (title: .sureToLeaveSeatText, message: ""), sureAction: { [weak self] in
                    guard let `self` = self else { return }
                    self.leaveSeat()
                }, cancelAction: {
                    
                })
            } else {
                if effectViewModel.currentPlayingModel == nil {
                    viewResponder?.showActionSheet(actionTitles: [.makeAudienceText], actions: { [weak self] (index) in
                        guard let `self` = self else { return }
                        if index == 0 {
                            // 下麦
                            self.chorusRoom.kickSeat(seatIndex: model.seatIndex, callback: nil)
                        }
                    })
                } else {
                    viewResponder?.showToast(message: .cannotleavetheseatText)
                }
            }
        } else {
            currentInvitateSeatIndex = model.seatIndex
            viewResponder?.showAudienceAlert(seat: model)
        }
    }
    
    private func notifyMsg(entity: MsgEntity) {
        DispatchQueue.main.async { [weak self] in
            guard let `self` = self else { return }
            if self.msgEntityList.count > 1000 {
                self.msgEntityList.removeSubrange(0...99)
            }
            self.msgEntityList.append(entity)
            self.viewResponder?.refreshMsgView()
        }
    }
    
    private func showNotifyMsg(messsage: String, userName: String, type: MsgEntityType = .normal, action: (() -> ())? = nil) {
        let msgEntity = MsgEntity.init(userId: "", userName: userName, content: messsage, invitedId: "", type: type, action: action)
        if msgEntityList.count > 1000 {
            msgEntityList.removeSubrange(0...99)
        }
        msgEntityList.append(msgEntity)
        viewResponder?.refreshMsgView()
    }
    
    private func showSelectedMusic(music: ChorusMusicModel) {
        var action: (() -> ())? = nil
        if isOwner {
            action = { [weak self] in
                guard let `self` = self else { return }
                self.effectViewModel.viewResponder?.onManageSongBtnClick()
            }
        }
        showNotifyMsg(messsage: LocalizeReplaceThreeCharacter(.xxSeatSelectzzSongText, "\(music.seatIndex + 1)", "xxx", music.musicName), userName: music.bookUserName, type: isOwner ? .manage_song : .normal, action: action)
    }
    
    /// 观众开始上麦
    /// - Parameter seatIndex: 上的作为号
    private func startTakeSeat(seatIndex: Int) {
        if isOwner {
            // 不需要的情况下自动上麦
            chorusRoom.enterSeat(seatIndex: seatIndex) { [weak self] (code, message) in
                guard let `self` = self else { return }
                if code == 0 {
                    self.viewResponder?.showToast(message: .handsupSuccessText)
                } else {
                    self.viewResponder?.showToast(message: .handsupFailedText)
                }
            }
        } else {
            // 需要申请上麦
            guard roomInfo.ownerId != "" else {
                viewResponder?.showToast(message: .roomNotReadyText)
                return
            }
            let cmd = ChorusConstants.CMD_REQUEST_TAKE_SEAT
            let targetUserId = roomInfo.ownerId
            let inviteId = chorusRoom.sendInvitation(cmd: cmd, userId: targetUserId, content: "\(seatIndex)") { [weak self] (code, message) in
                guard let `self` = self else { return }
                if code == 0 {
                    self.viewResponder?.showToast(message: .reqSentText)
                } else {
                    self.viewResponder?.showToast(message: LocalizeReplaceXX(.reqSendFailedxxText, message))
                }
            }
            currentInvitateSeatIndex = seatIndex
            mInvitationSeatDic[inviteId] = seatIndex
        }
    }
    
    private func changeAudience(status: Int, user: ChorusUserInfo) {
        guard [AudienceInfoModel.TYPE_IDEL, AudienceInfoModel.TYPE_IN_SEAT, AudienceInfoModel.TYPE_WAIT_AGREE].contains(status) else { return }
        if isOwner && roleType == .anchor {
            let audience = memberAudienceDic[user.userId]
            if var audienceModel = audience {
                if audienceModel.type == status { return }
                audienceModel.type = status
                memberAudienceDic[audienceModel.userInfo.userId] = audienceModel
                if let index = memberAudienceList.firstIndex(where: { (model) -> Bool in
                    return model.userInfo.userId == audienceModel.userInfo.userId
                }) {
                    memberAudienceList[index] = audienceModel
                }
            }
        }
        viewResponder?.audienceListRefresh()
    }
    
    private func recvPickSeat(identifier: String, cmd: String, content: String) {
        guard let seatIndex = Int.init(content) else { return }
        viewResponder?.showAlert(info: (title: .alertText, message: LocalizeReplaceXX(.invitexxSeatText, String(seatIndex))), sureAction: { [weak self] in
            guard let `self` = self else { return }
            self.chorusRoom.acceptInvitation(identifier: identifier) { [weak self] (code, message) in
                guard let `self` = self else { return }
                if code != 0 {
                    self.viewResponder?.showToast(message: .acceptReqFailedText)
                }
            }
        }, cancelAction: { [weak self] in
            guard let `self` = self else { return }
            self.chorusRoom.rejectInvitation(identifier: identifier) { [weak self] (code, message) in
                guard let `self` = self else { return }
                self.viewResponder?.showToast(message: .refuseHandsupText)
            }
        })
    }
    
    private func recvTakeSeat(identifier: String, inviter: String, content: String) {
        // 收到新的邀请后，更新列表,其他的信息
        if let index = msgEntityList.firstIndex(where: { (msg) -> Bool in
            return msg.userId == inviter && msg.type == .wait_agree
        }) {
            var msg = msgEntityList[index]
            msg.type = .agreed
            msgEntityList[index] = msg
        }
        // 显示到通知栏
        let audinece = memberAudienceDic[inviter]
        let seatIndex = (Int.init(content) ?? 0)
        let content = LocalizeReplaceXX(.applyxxSeatText, String(seatIndex + 1))
        let msgEntity = MsgEntity.init(userId: inviter, userName: audinece?.userInfo.userName ?? inviter, content: content, invitedId: identifier, type: .wait_agree)
        msgEntityList.append(msgEntity)
        viewResponder?.refreshMsgView()
        if var audienceModel = audinece {
            audienceModel.type = AudienceInfoModel.TYPE_WAIT_AGREE
            memberAudienceDic[audienceModel.userInfo.userId] = audienceModel
            if let index = memberAudienceList.firstIndex(where: { (model) -> Bool in
                return model.userInfo.userId == audienceModel.userInfo.userId
            }) {
                memberAudienceList[index] = audienceModel
            }
            viewResponder?.audienceListRefresh()
        }
        mTakeSeatInvitationDic[inviter] = identifier
    }
}

//MARK: - public method
extension TRTCChorusViewModel {
    public func createRoom(toneQuality: Int = 0) {
        let faceUrl = TRTCChorusIMManager.sharedManager().curUserAvatar
        chorusRoom.setAuidoQuality(quality: toneQuality)
        chorusRoom.setSelfProfile(userName: roomInfo.ownerName, avatarURL: faceUrl) { [weak self] (code, message) in
            guard let `self` = self else { return }
            TRTCLog.out("setSelfProfile\(code):\(message)")
            self.dependencyContainer.createRoom(roomID: "\(self.roomInfo.roomID)") {  [weak self] in
                guard let `self` = self else { return }
                self.internalCreateRoom()
            } failed: { [weak self] code, message in
                guard let `self` = self else { return }
                if code == -1301 {
                    self.internalCreateRoom()
                } else {
                    self.viewResponder?.showToast(message: .createRoomFailedText)
                    self.viewNavigator?.popToPrevious()
                }
            }
        }
    }
    
    public func enterRoom(toneQuality: Int = ChorusToneQuality.defaultQuality.rawValue) {
        chorusRoom.enterRoom(roomID: roomInfo.roomID) { [weak self] (code, message) in
            guard let `self` = self else { return }
            if code == 0 {
                self.viewResponder?.showToast(message: .enterSuccessText)
                self.chorusRoom.setAuidoQuality(quality: toneQuality)
                self.getAudienceList()
            } else {
                self.viewResponder?.showToast(message: .enterFailedText)
                self.viewNavigator?.popToPrevious()
            }
        }
    }
    
    public func refreshView() {
        if let viewResponder = viewResponder {
            viewResponder.switchView(type: userType)
        }
    }
    
    public func clickSeat(model: SeatInfoModel) {
        guard isSeatInitSuccess else {
            viewResponder?.showToast(message: .seatuninitText)
            return
        }
        if isOwner {
            ownerClickItem(model: model)
        }
        else {
            audienceClickItem(model: model)
        }
    }
    
    public func leaveSeat() {
        chorusRoom.leaveSeat { [weak self] (code, message) in
            guard let `self` = self else { return }
            if code == 0 {
                self.viewResponder?.showToast(message: .audienceSuccessText)
            } else {
                self.viewResponder?.showToast(message: LocalizeReplaceXX(.audienceFailedxxText, message))
            }
        }
    }
    
    public func exitRoom(completion: @escaping (() -> ())) {
        guard !isExitingRoom else { return }
        musicDataSource?.onExitRoom()
        viewNavigator?.popToPrevious()
        isExitingRoom = true
        if isOwner && roleType == .anchor {
            dependencyContainer.destroyRoom(roomID: "\(roomInfo.roomID)", success: {
                TRTCLog.out("---deinit room success")
            }) { (code, message) in
                TRTCLog.out("---deinit room failed")
            }
            chorusRoom.destroyRoom { [weak self] (code, message) in
                guard let `self` = self else { return }
                self.isExitingRoom = false
                completion()
            }
            return
        }
        chorusRoom.exitRoom { [weak self] (code, message) in
            guard let `self` = self else { return }
            self.isExitingRoom = false
            completion()
        }
    }
    
    public func getRealMemberAudienceList() -> [AudienceInfoModel] {
        var res : [AudienceInfoModel] = []
        for audience in memberAudienceList {
            if memberAudienceDic.keys.contains(audience.userInfo.userId) {
                res.append(audience)
            }
        }
        return res
    }
    
    public func spechAction(isMute: Bool) {
        chorusRoom.muteAllRemoteAudio(isMute: isMute)
        if isMute {
            viewResponder?.showToast(message: .mutedText)
        } else {
            viewResponder?.showToast(message: .unmutedText)
        }
    }
    
    public func acceptTakeSeat(identifier: String) {
        if let audience = memberAudienceDic[identifier] {
            acceptTakeSeatInviation(userInfo: audience.userInfo)
        }
    }
    
    public func onTextMsgSend(message: String) {
        if message.count == 0 {
            return
        }
        // 消息回显示
        let entity = MsgEntity.init(userId: TRTCChorusIMManager.sharedManager().curUserID, userName: .meText, content: message, invitedId: "", type: .normal)
        notifyMsg(entity: entity)
        chorusRoom.sendRoomTextMsg(message: message) { [weak self] (code, message) in
            guard let `self` = self else { return }
            self.viewResponder?.showToast(message: code == 0 ? .sendSuccessText :  LocalizeReplaceXX(.sendFailedText, message))
        }
    }
    
    public func openMessageTextInput() {
        viewResponder?.msgInput(show: true)
    }
    
    public func muteAction(isMute: Bool) -> Bool {
        guard !isOwnerMute else {
            viewResponder?.showToast(message: .seatmutedText)
            return false
        }
        isSelfMute = isMute
        chorusRoom.muteLocalAudio(mute: isMute)
        if isMute {
            viewResponder?.showToast(message: .micmutedText)
        } else {
            viewResponder?.recoveryVoiceSetting()
            viewResponder?.showToast(message: .micunmutedText)
        }
        return true
    }
    
    public func sendGift(giftId: String, callback: ActionCallback?) {
        let giftMsgInfo = TUIGiftMsgInfo.init(giftId: giftId, sendUser: TRTCChorusIMManager.sharedManager().curUserName, sendUserHeadIcon: TRTCChorusIMManager.sharedManager().curUserAvatar)
        do {
            let encoder = JSONEncoder.init()
            let data = try encoder.encode(giftMsgInfo)
            let message = String.init(decoding: data, as: UTF8.self)
            chorusRoom.sendRoomCustomMsg(cmd: kSendGiftCmd, message: message, callback: callback)
        } catch {
            
        }
    }
    
    public func showAlert(viewController: UIViewController, animated: Bool, completion: (() -> Void)?) {
        if let viewNavigator = viewNavigator {
            viewNavigator.presentAlert(viewController: viewController, animated: animated, completion: completion)
        }
    }
    
    public func clickSeatLock(isLock: Bool, model: SeatInfoModel) {
        self.chorusRoom.closeSeat(seatIndex: model.seatIndex, isClose: isLock, callback: nil)
    }
    
}

extension TRTCChorusViewModel: ChorusMusicServiceDelegate {
    
    func onShouldSetLyric(musicID: String) {
        if musicID == "0" {
            viewResponder?.onHiddenChorusGifAnimation()
        } else {
            viewResponder?.onShowChorusGifAnimation()
        }
        effectViewModel.viewResponder?.bgmOnPrepareToPlay(musicID: Int32(musicID) ?? 0)
    }
    
    func onMusicListChange(musicInfoList: [ChorusMusicModel], reason: Int) {
        effectViewModel.musicSelectedList = musicInfoList
        effectViewModel.musicList.forEach { (model) in
            model.isSelected = false
            model.action = effectViewModel.listAction
            for (i, info) in musicInfoList.enumerated() {
                if info.musicID == model.musicID {
                    model.isSelected = true
                    model.action = effectViewModel.selectedAction
                    let smodel = effectViewModel.musicSelectedList[i]
                    smodel.isSelected = true
                    smodel.action = effectViewModel.selectedAction
                    for seat in anchorSeatList {
                        if let user = seat.seatUser {
                            if user.userId == smodel.bookUserID {
                                smodel.seatIndex = seat.seatIndex
                                smodel.bookUserName = user.userName
                                smodel.bookUserAvatar = user.userAvatar
                                break
                            }
                        }
                    }
                    break
                }
            }
        }
        effectViewModel.viewResponder?.onSelectedMusicListChanged()
        effectViewModel.viewResponder?.onMusicListChanged()
    }
    
    func onShouldPlay(_ music: ChorusMusicModel) {
        if mSelfSeatIndex >= 0 {
            currentMusicModel = music
        }
    }
    
    func onShouldStopPlay(_ music: ChorusMusicModel) {
        if effectViewModel.currentPlayingModel == nil {
            musicDataSource?.completePlaying(musicID: "\(music.musicID)")
            return
        }
        effectViewModel.stopPlay()
    }
    
    func onShouldShowMessage(_ music: ChorusMusicModel) {
        for seat in anchorSeatList {
            if let user = seat.seatUser, user.userId == music.bookUserID {
                music.seatIndex = seat.seatIndex
                music.bookUserName = user.userName
                music.bookUserAvatar = user.userAvatar
            }
        }
        showSelectedMusic(music: music)
    }
}

extension TRTCChorusViewModel: TRTCChorusRoomDelegate {
    func onReceiveAnchorSendChorusMsg(musicId: String, startDelay: Int) {
        guard let musicID = Int32(musicId) else {
            return
        }
        if userType == .audience { return }
        musicDataSource?.chorusGetMusicPage(page: 0, pageSize: 0, callback: { [weak self] musicInfos in
            guard let `self` = self else { return }
            musicInfos.forEach { [weak self] musicModel in
                guard let `self` = self else { return }
                if musicModel.musicID == musicID {
                    self.effectViewModel.viewResponder?.showStartAnimationAndPlay(startDelay: startDelay)
                    self.effectViewModel.setCurrentMusicEffect()
                    self.chorusRoom.startPlayMusic(musicID: musicModel.musicID, url: musicModel.contentUrl)
                    return
                }
            }
        })
    }
    
    func onNetworkQuality(local localQuality: TRTCQualityInfo, remote remoteQuality: [TRTCQualityInfo]) {
        viewResponder?.updateLocalNetwork(network: localQuality.quality.rawValue)
        if remoteQuality.count > 0 {
            remoteQuality.forEach { remoteQuality in
                viewResponder?.updateRemoteNetwork(network: remoteQuality.quality.rawValue)
            }
        } else {
            viewResponder?.updateRemoteNetwork(network: 0)
        }
    }
    
    func onError(code: Int32, message: String) {
        
    }
    
    func onWarning(code: Int32, message: String) {
        
    }
    
    func onDebugLog(message: String) {
        
    }
    
    func onRoomDestroy(message: String) {
        if TRTCChorusFloatingWindowManager.shared().windowIsShowing {
            TRTCChorusFloatingWindowManager.shared().closeWindowAndExitRoom {
                if let window = UIApplication.shared.windows.first {
                    window.makeToast(.closeRoomText)
                }
            }
        } else {
            if let window = UIApplication.shared.windows.first {
                window.makeToast(.closeRoomText)
            }
            viewResponder?.showToast(message: .closeRoomText)
            chorusRoom.exitRoom(callback: nil)
            viewNavigator?.popToPrevious()
        }
#if RTCube_APPSTORE
        guard isOwner else { return }
        let selector = NSSelectorFromString("showAlertUserLiveTimeOut")
        if UIViewController.responds(to: selector) {
            UIViewController.perform(selector)
        }
#endif
    }
    
    func onRoomInfoChange(roomInfo: ChorusRoomInfo) {
        // 值为-1表示该接口没有返回数量信息
        if roomInfo.memberCount == -1 {
            roomInfo.memberCount = self.roomInfo.memberCount
        }
        self.roomInfo = roomInfo
        viewResponder?.changeRoom(info: self.roomInfo)
        musicDataSource?.setRoomInfo(roomInfo: roomInfo)
    }
    
    func onSeatListChange(seatInfoList seatInfolist: [ChorusSeatInfo]) {
        TRTCLog.out("roomLog: onSeatListChange: \(seatInfolist)")
        isSeatInitSuccess = true
        seatInfolist.enumerated().forEach { (item) in
            let seatIndex = item.offset
            let seatInfo = item.element
            var anchorSeatInfo = SeatInfoModel.init { [weak self] (seatIndex) in
                guard let `self` = self else { return }
                if seatIndex >= 0 && seatIndex <= self.anchorSeatList.count {
                    let model = self.anchorSeatList[seatIndex]
                    self.clickSeat(model: model)
                }
            }
            anchorSeatInfo.seatInfo = seatInfo
            anchorSeatInfo.isUsed = seatInfo.status == 1
            anchorSeatInfo.isClosed = seatInfo.status == 2
            anchorSeatInfo.seatIndex = seatIndex
            anchorSeatInfo.isOwner = roomInfo.ownerId == TRTCChorusIMManager.sharedManager().curUserID
            let listIndex = seatIndex
            if anchorSeatList.count == seatInfolist.count {
                // 说明有数据
                let anchorSeatModel = anchorSeatList[listIndex]
                anchorSeatInfo.seatUser = anchorSeatModel.seatUser
                if !anchorSeatInfo.isUsed {
                    anchorSeatInfo.seatUser = nil
                }
                anchorSeatList[listIndex] = anchorSeatInfo
            } else {
                // 说明没数据
                anchorSeatList.append(anchorSeatInfo)
            }
        }
        let seatUserIds = seatInfolist.filter({ (seat) -> Bool in
            return seat.userId != ""
        }).map { (seatInfo) -> String in
            return seatInfo.userId
        }
        guard seatUserIds.count > 0 else {
            viewResponder?.refreshAnchorInfos()
            return
        }
        chorusRoom.getUserInfoList(userIDList: seatUserIds) { [weak self] (code, message, userInfos) in
            guard let `self` = self else { return }
            guard code == 0 else { return }
            var userdic: [String : ChorusUserInfo] = [:]
            userInfos.forEach { (info) in
                userdic[info.userId] = info
            }
            if seatInfolist.count == 0 {
                return
            }
            if self.anchorSeatList.count != seatInfolist.count {
                TRTCLog.out(String.seatlistWrongText)
                return
            }
            // 修改座位列表的user信息
            for index in 0..<self.anchorSeatList.count {
                let seatInfo = seatInfolist[index] // 从观众开始更新
                if self.anchorSeatList[index].seatUser == nil, let user = userdic[seatInfo.userId], !self.userMuteMap.keys.contains(user.userId) {
                    self.userMuteMap[user.userId] = true
                }
                self.anchorSeatList[index].seatUser = userdic[seatInfo.userId]
            }
            self.viewResponder?.refreshAnchorInfos()
            self.viewResponder?.onAnchorMute(isMute: false)
        }
    }
    
    func onSeatClose(index: Int, isClose: Bool) {
        showNotifyMsg(messsage: LocalizeReplace(.ownerxxSeatText, isClose ? .banSeatText : .unmuteOneText, String(index + 1)), userName: "")
    }
    
    func onAnchorEnterSeat(index: Int, user: ChorusUserInfo) {
        showNotifyMsg(messsage: LocalizeReplace(.beyySeatText, "xxx", String(index + 1)), userName: user.userName)
        if user.userId == TRTCChorusIMManager.sharedManager().curUserID {
            if index == 0 {
                userType = .anchor
            } else {
                userType = .chorus
            }
            refreshView()
            mSelfSeatIndex = index
            viewResponder?.recoveryVoiceSetting() // 自己上麦，恢复音效设置
        }
        userMuteMap[user.userId] = false
        changeAudience(status: AudienceInfoModel.TYPE_IN_SEAT, user: user)
    }
    
    func onAnchorLeaveSeat(index: Int, user: ChorusUserInfo) {
        showNotifyMsg(messsage: LocalizeReplace(.audienceyySeatText, "xxx", String(index + 1)), userName: user.userName)
        if user.userId == TRTCChorusIMManager.sharedManager().curUserID {
            userType = .audience
            refreshView()
            mSelfSeatIndex = -1
            isOwnerMute = false
            // 自己下麦，停止音效播放
            effectViewModel.stopPlay()
            musicDataSource?.deleteAllMusic(userID: TRTCChorusIMManager.sharedManager().curUserID, callback: { (code, msg) in
                
            })
        }
        if !memberAudienceDic.keys.contains(user.userId) {
            for model in memberAudienceList {
                if model.userInfo.userId == user.userId {
                    memberAudienceDic[user.userId] = model
                    break
                }
            }
        }
        changeAudience(status: AudienceInfoModel.TYPE_IDEL, user: user)
    }
    
    func onUserMicrophoneMute(userId: String, mute: Bool) {
        
    }
    
    func onAudienceEnter(userInfo: ChorusUserInfo) {
        showNotifyMsg(messsage: LocalizeReplaceXX(.inRoomText, "xxx"), userName: userInfo.userName)
        // 主播端(房主)
        let memberEntityModel = AudienceInfoModel.init(type: 0, userInfo: userInfo) { [weak self] (index) in
            guard let `self` = self else { return }
            if index == 0 {
                self.sendInvitation(userInfo: userInfo)
            } else {
                self.acceptTakeSeatInviation(userInfo: userInfo)
                self.viewResponder?.audiceneList(show: false)
            }
        }
        if !memberAudienceDic.keys.contains(userInfo.userId) {
            memberAudienceDic[userInfo.userId] = memberEntityModel
            memberAudienceList.append(memberEntityModel)
        }
        viewResponder?.audienceListRefresh()
        changeAudience(status: AudienceInfoModel.TYPE_IDEL, user: userInfo)
    }
    
    func onAudienceExit(userInfo: ChorusUserInfo) {
        showNotifyMsg(messsage: LocalizeReplaceXX(.exitRoomText, "xxx"), userName: userInfo.userName)
        memberAudienceList.removeAll { (model) -> Bool in
            return model.userInfo.userId == userInfo.userId
        }
        memberAudienceDic.removeValue(forKey: userInfo.userId)
        viewResponder?.refreshAnchorInfos()
        viewResponder?.audienceListRefresh()
        changeAudience(status: AudienceInfoModel.TYPE_IDEL, user: userInfo)
    }
    
    func onUserVolumeUpdate(userVolumes: [TRTCVolumeInfo], totalVolume: Int) {
        var volumeDic: [String: UInt] = [:]
        userVolumes.forEach { (info) in
            if let userId = info.userId {
                volumeDic[userId] = info.volume
            } else {
                volumeDic[TRTCChorusIMManager.sharedManager().curUserID] = info.volume
            }
        }
        var needRefreshUI = false
        for (index, seat) in self.anchorSeatList.enumerated() {
            if let user = seat.seatUser {
                let isTalking = (volumeDic[user.userId] ?? 0) > 25
                if seat.isTalking != isTalking {
                    self.anchorSeatList[index].isTalking = isTalking
                    needRefreshUI = true
                }
            }
        }
        
        if needRefreshUI {
            viewResponder?.refreshAnchorInfos()
        }
    }
    
    func onRecvRoomTextMsg(message: String, userInfo: ChorusUserInfo) {
        let msgEntity = MsgEntity.init(userId: userInfo.userId,
                                       userName: userInfo.userName,
                                       content: message,
                                       invitedId: "",
                                       type: .normal)
        notifyMsg(entity: msgEntity)
    }
    
    func onRecvRoomCustomMsg(cmd: String, message: String, userInfo: ChorusUserInfo) {
        if cmd == kSendGiftCmd {
            // 收到发送礼物的自定义消息
            guard let data = message.data(using: .utf8) else { return }
            let decoder = JSONDecoder.init()
            if let giftMsgInfo = try? decoder.decode(TUIGiftMsgInfo.self, from: data) {
                if let responder = viewResponder {
                    if let giftModel = giftManager.getGiftModel(giftId: giftMsgInfo.giftId) {
                        responder.showGiftAnimation(giftInfo: TUIGiftInfo.init(giftModel: giftModel, sendUser: giftMsgInfo.sendUser, sendUserHeadIcon: giftMsgInfo.sendUserHeadIcon))
                    }
                }
            } else {
                if let responder = viewResponder {
                    if let giftModel = giftManager.getGiftModel(giftId: message) {
                        responder.showGiftAnimation(giftInfo: TUIGiftInfo.init(giftModel: giftModel, sendUser: "", sendUserHeadIcon: ""))
                    }
                }
            }
        }
    }
    
    func onReceiveNewInvitation(identifier: String, inviter: String, cmd: String, content: String) {
        TRTCLog.out("receive message: \(cmd) : \(content)")
        if roleType == .audience {
            if cmd == ChorusConstants.CMD_PICK_UP_SEAT {
                recvPickSeat(identifier: identifier, cmd: cmd, content: content)
            }
        }
        if roleType == .anchor && isOwner {
            if cmd == ChorusConstants.CMD_REQUEST_TAKE_SEAT {
                recvTakeSeat(identifier: identifier, inviter: inviter, content: content)
            }
        }
    }
    
    func onInviteeAccepted(identifier: String, invitee: String) {
        if roleType == .audience {
            guard let seatIndex = mInvitationSeatDic.removeValue(forKey: identifier) else {
                return
            }
            guard let seatModel = anchorSeatList.filter({ (seatInfo) -> Bool in
                return seatInfo.seatIndex == seatIndex
            }).first else {
                return
            }
            if !seatModel.isUsed {
                // 显示Loading指示框， 回调结束消失
                self.viewResponder?.showToastActivity()
                chorusRoom.enterSeat(seatIndex: seatIndex) { [weak self] (code, message) in
                    guard let `self` = self else { return }
                    // 隐藏Loading指示器
                    self.viewResponder?.hiddenToastActivity()
                    if code == 0 {
                        self.viewResponder?.showToast(message: .handsupSuccessText)
                    } else {
                        self.viewResponder?.showToast(message: .handsupFailedText)
                    }
                }
            }
        }
        if roleType == .anchor && isOwner {
            guard let seatInvitation = mPickSeatInvitationDic.removeValue(forKey: identifier) else {
                return
            }
            guard let seatModel = anchorSeatList.filter({ (model) -> Bool in
                return model.seatIndex == seatInvitation.seatIndex
            }).first else {
                return
            }
            if !seatModel.isUsed {
                chorusRoom.pickSeat(seatIndex: seatInvitation.seatIndex, userId: seatInvitation.inviteUserId) { [weak self] (code, message) in
                    guard let `self` = self else { return }
                    if code == 0 {
                        guard let audience = self.memberAudienceDic[seatInvitation.inviteUserId] else { return }
                        self.viewResponder?.showToast(message: LocalizeReplaceXX(.hugHandsupSuccessText, audience.userInfo.userName))
                    }
                }
            }
        }
    }
    
    func onInviteeRejected(identifier: String, invitee: String) {
        if let seatInvitation = mPickSeatInvitationDic.removeValue(forKey: identifier) {
            guard let audience = memberAudienceDic[seatInvitation.inviteUserId] else { return }
            viewResponder?.showToast(message: LocalizeReplaceXX(.refuseBespeakerText, audience.userInfo.userName))
            changeAudience(status: AudienceInfoModel.TYPE_IDEL, user: audience.userInfo)
        }
    }
    
    func onInvitationCancelled(identifier: String, invitee: String) {
        
    }
    
    func onMusicProgressUpdate(musicID: Int32, progress: Int, total: Int) {
        effectViewModel.viewResponder?.bgmOnPlaying(musicID: musicID, current: Double(progress) / 1000.0, total: Double(total) / 1000.0)
    }
    
    func onMusicPrepareToPlay(musicID: Int32) {
        effectViewModel.viewResponder?.bgmOnPrepareToPlay(musicID: musicID)
        musicDataSource?.prepareToPlay(musicID: String(musicID))
    }
    
    func onMusicCompletePlaying(musicID: Int32) {
        effectViewModel.currentPlayingModel = nil
        musicDataSource?.completePlaying(musicID: String(musicID))
    }
    
}

/// MARK: - internationalization string
fileprivate extension String {
    static let seatmutedText = ChorusLocalize("Demo.TRTC.Chorus.onseatmuted")
    static let micmutedText = ChorusLocalize("Demo.TRTC.Salon.micmuted")
    static let micunmutedText = ChorusLocalize("Demo.TRTC.Salon.micunmuted")
    static let mutedText = ChorusLocalize("Demo.TRTC.Chorus.ismuted")
    static let unmutedText = ChorusLocalize("Demo.TRTC.Chorus.isunmuted")
    static let seatuninitText = ChorusLocalize("Demo.TRTC.Salon.seatlistnotinit")
    static let enterSuccessText = ChorusLocalize("Demo.TRTC.Salon.enterroomsuccess")
    static let enterFailedText = ChorusLocalize("Demo.TRTC.Salon.enterroomfailed")
    static let createRoomFailedText = ChorusLocalize("Demo.TRTC.LiveRoom.createroomfailed")
    static let meText = ChorusLocalize("Demo.TRTC.LiveRoom.me")
    static let sendSuccessText = ChorusLocalize("Demo.TRTC.Chorus.sendsuccess")
    static let sendFailedText = ChorusLocalize("Demo.TRTC.Chorus.sendfailedxx")
    static let cupySeatSuccessText = ChorusLocalize("Demo.TRTC.Salon.hostoccupyseatsuccess")
    static let cupySeatFailedText = ChorusLocalize("Demo.TRTC.Salon.hostoccupyseatfailed")
    static let onlyAnchorOperationText = ChorusLocalize("Demo.TRTC.Chorus.onlyanchorcanoperation")
    static let seatLockedText = ChorusLocalize("Demo.TRTC.Chorus.seatislockedandcanthandup")
    static let audienceText = ChorusLocalize("Demo.TRTC.Salon.audience")
    static let otherAnchorText = ChorusLocalize("Demo.TRTC.Chorus.otheranchor")
    static let isInxxSeatText = ChorusLocalize("Demo.TRTC.Chorus.isinxxseat")
    static let notInitText = ChorusLocalize("Demo.TRTC.Chorus.seatisnotinittocanthandsup")
    static let handsupText = ChorusLocalize("Demo.TRTC.Salon.handsup")
    static let totaxxText = ChorusLocalize("Demo.TRTC.Chorus.totaxx")
    static let unmuteOneText = ChorusLocalize("Demo.TRTC.Chorus.unmuteone")
    static let muteOneText = ChorusLocalize("Demo.TRTC.Chorus.muteone")
    static let makeAudienceText = ChorusLocalize("Demo.TRTC.Chorus.makeoneaudience")
    static let inviteHandsupText = ChorusLocalize("Demo.TRTC.Chorus.invitehandsup")
    static let banSeatText = ChorusLocalize("Demo.TRTC.Chorus.banseat")
    static let liftbanSeatText = ChorusLocalize("Demo.TRTC.Chorus.liftbanseat")
    static let seatBusyText = ChorusLocalize("Demo.TRTC.Chorus.seatisbusy")
    static let sendInviteSuccessText = ChorusLocalize("Demo.TRTC.Chorus.sendinvitesuccess")
    static let reqExpiredText = ChorusLocalize("Demo.TRTC.Salon.reqisexpired")
    static let acceptReqFailedText = ChorusLocalize("Demo.TRTC.Salon.acceptreqfailed")
    static let audienceSuccessText = ChorusLocalize("Demo.TRTC.Salon.audiencesuccess")
    static let audienceFailedxxText = ChorusLocalize("Demo.TRTC.Salon.audiencefailedxx")
    static let beingArchonText = ChorusLocalize("Demo.TRTC.Salon.isbeingarchon")
    static let roomNotReadyText = ChorusLocalize("Demo.TRTC.Salon.roomnotready")
    static let reqSentText = ChorusLocalize("Demo.TRTC.Chorus.reqsentandwaitforarchondeal")
    static let reqSendFailedxxText = ChorusLocalize("Demo.TRTC.Chorus.reqsendfailedxx")
    static let handsupSuccessText = ChorusLocalize("Demo.TRTC.Salon.successbecomespaker")
    static let handsupFailedText = ChorusLocalize("Demo.TRTC.Salon.failedbecomespaker")
    
    static let alertText = ChorusLocalize("Demo.TRTC.LiveRoom.prompt")
    static let invitexxSeatText = ChorusLocalize("Demo.TRTC.Chorus.anchorinvitexxseat")
    static let refuseHandsupText = ChorusLocalize("Demo.TRTC.Chorus.refusehandsupreq")
    static let applyxxSeatText = ChorusLocalize("Demo.TRTC.Chorus.applyforxxseat")
    static let closeRoomText = ChorusLocalize("Demo.TRTC.Salon.archonclosedroom")
    static let seatlistWrongText = ChorusLocalize("Demo.TRTC.Chorus.seatlistwentwrong")
    static let beyySeatText = ChorusLocalize("Demo.TRTC.Chorus.xxbeyyseat")
    static let audienceyySeatText = ChorusLocalize("Demo.TRTC.Chorus.xxaudienceyyseat")
    static let bemutedxxText = ChorusLocalize("Demo.TRTC.Chorus.xxisbemuted")
    static let beunmutedxxText = ChorusLocalize("Demo.TRTC.Chorus.xxisbeunmuted")
    static let ownerxxSeatText = ChorusLocalize("Demo.TRTC.Chorus.ownerxxyyseat")
    static let banText = ChorusLocalize("Demo.TRTC.Chorus.ban")
    static let inRoomText = ChorusLocalize("Demo.TRTC.LiveRoom.xxinroom")
    static let exitRoomText = ChorusLocalize("Demo.TRTC.Chorus.xxexitroom")
    static let hugHandsupSuccessText = ChorusLocalize("Demo.TRTC.Chorus.hugxxhandsupsuccess")
    static let refuseBespeakerText = ChorusLocalize("Demo.TRTC.Chorus.refusebespeaker")
    static let sureToLeaveSeatText = ChorusLocalize("Demo.TRTC.Chorus.alertdeleteallmusic")
    static let takeSeatText = ChorusLocalize("Demo.TRTC.Chorus.micon")
    static let lockSeatText = ChorusLocalize("Demo.TRTC.Chorus.lockseat")
    static let unlockSeatText = ChorusLocalize("Demo.TRTC.Chorus.unlockseat")
    static let xxSeatSelectzzSongText = ChorusLocalize("Demo.TRTC.Chorus.xxmicyyselectzz")
    static let cannotleavetheseatText = ChorusLocalize("Demo.TRTC.Chorus.cannotleavetheseat")
}
