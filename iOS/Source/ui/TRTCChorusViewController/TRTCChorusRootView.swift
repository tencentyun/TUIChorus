//
//  TRTCChorusRootView.swift
//  Alamofire
//
//  Created by adams on 2021/7/14.
//

import UIKit
import Kingfisher
import Toast_Swift
import TXAppBasic

class TRTCChorusRootView: UIView {
    private var isViewReady: Bool = false
    private let viewModel: TRTCChorusViewModel
    
    lazy var giftAnimator: TUIGiftAnimator = {
        let giftAnimator = TUIGiftAnimator.init(animationContainerView: self)
        return giftAnimator
    }()
    
    let backgroundLayer: CALayer = {
        // fillCode
        let layer = CAGradientLayer()
        layer.colors = [UIColor.init(0x13294b).cgColor, UIColor.init(0x000000).cgColor]
        layer.locations = [0.2, 1.0]
        layer.startPoint = CGPoint(x: 0.4, y: 0)
        layer.endPoint = CGPoint(x: 0.6, y: 1.0)
        return layer
    }()
    
    lazy var bgView: UIView = {
        let bg = UIView(frame: .zero)
        return bg
    }()
    
    lazy var topView : TRTCChorusTopView = {
        var view = TRTCChorusTopView(viewModel: viewModel)
        return view
    }()

    lazy var lyricView: TRTCLyricView = {
        let view = TRTCLyricView(viewModel: viewModel)
        return view
    }()
    
    lazy var tipsView: TRTCChorusTipsView = {
        let view = TRTCChorusTipsView.init(frame: .zero, viewModel: viewModel)
        return view
    }()
    
    lazy var chorusSeatsView: TRTCChorusSeatsView = {
        let view = TRTCChorusSeatsView.init(frame: .zero, viewModel: viewModel)
        view.backgroundColor = .clear
        return view
    }()
    
    lazy var mainMenuView: TRTCChorusMainMenuView = {
        let icons: [IconTuple] = [
            IconTuple(normal: UIImage(named: "room_message", in: ChorusBundle(), compatibleWith: nil)!, selected: UIImage(named: "room_message", in: ChorusBundle(), compatibleWith: nil)!, type: .message),
            IconTuple(normal: UIImage(named: "room_leave_mic", in: ChorusBundle(), compatibleWith: nil)!, selected: UIImage(named: "room_leave_mic", in: ChorusBundle(), compatibleWith: nil)!, type: .micoff),
            IconTuple(normal: UIImage(named: "room_voice_off", in: ChorusBundle(), compatibleWith: nil)!, selected: UIImage(named: "room_voice_on", in: ChorusBundle(), compatibleWith: nil)!, type: .mute),
            IconTuple(normal: UIImage(named: "gift", in: ChorusBundle(), compatibleWith: nil)!, selected: UIImage(named: "gift", in: ChorusBundle(), compatibleWith: nil)!, type: .gift),
        ]
        icons.forEach { (icon) in
            switch icon.type {
            case .mute:
                viewModel.muteItem = icon
            default:
                break
            }
        }
        let view = TRTCChorusMainMenuView.init(icons: icons)
        return view
    }()
    
    lazy var msgInputView: TRTCChorusMsgInputView = {
        let view = TRTCChorusMsgInputView.init(frame: .zero, viewModel: viewModel)
        view.isHidden = true
        return view
    }()
    
    lazy var audiceneListView: TRTCChorusAudienceListView = {
        let view = TRTCChorusAudienceListView.init(viewModel: viewModel)
        view.hide()
        return view
    }()
    
    deinit {
        lyricView.cleanTimer()
        TRTCLog.out("reset audio settings")
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        let bgGradientLayer = bgView.gradient(colors: [UIColor(hex: "FF88DD")!.cgColor, UIColor(hex: "1E009B")!.cgColor])
        bgGradientLayer.startPoint = CGPoint(x: 0.8, y: 0)
        bgGradientLayer.endPoint = CGPoint(x: 0.2, y: 1)
    }
    
    init(frame: CGRect = .zero, viewModel: TRTCChorusViewModel) {
        self.viewModel = viewModel
        super.init(frame: frame)
        self.viewModel.viewResponder = self
        bindInteraction()
    }
    
    required init?(coder: NSCoder) {
        fatalError("can't init this viiew from coder")
    }
    
    // MARK: - 视图生命周期
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else {
            return
        }
        isViewReady = true
        constructViewHierarchy() // 视图层级布局
        activateConstraints() // 生成约束（此时有可能拿不到父视图正确的frame）
    }
    
    func constructViewHierarchy() {
        /// 此方法内只做add子视图操作
        backgroundLayer.frame = bounds;
        layer.insertSublayer(backgroundLayer, at: 0)
        addSubview(bgView)
        addSubview(topView)
        addSubview(lyricView)
        addSubview(chorusSeatsView)
        addSubview(tipsView)
        addSubview(mainMenuView)
        addSubview(msgInputView)
        addSubview(audiceneListView)
    }
    
    func activateConstraints() {
        bgView.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        topView.snp.makeConstraints { (make) in
            make.top.leading.trailing.equalToSuperview()
        }
        lyricView.snp.makeConstraints { (make) in
            make.top.equalTo(topView.snp.bottom)
            make.leading.equalToSuperview().offset(20)
            make.trailing.equalToSuperview().offset(-20)
            make.height.equalTo(189.0/812*ScreenHeight)
        }
        activateConstraintsOfCustomSeatArea()
        activateConstraintsOfTipsView()
        activateConstraintsOfMainMenu()
        activateConstraintsOfTextView()
        activateConstraintsOfAudiceneList()
    }
    
    func bindInteraction() {
        /// 此方法负责做viewModel和视图的绑定操作
        mainMenuView.delegate = self
    }
}

extension TRTCChorusRootView {
    func activateConstraintsOfCustomSeatArea() {
        chorusSeatsView.snp.makeConstraints { (make) in
            make.top.equalTo(lyricView.snp.bottom).offset(8)
            make.height.equalTo(78*2+8)
            make.left.equalToSuperview()
            make.right.equalToSuperview()
        }
    }
    
    func activateConstraintsOfTipsView() {
        tipsView.snp.makeConstraints { (make) in
            make.top.equalTo(chorusSeatsView.snp.bottom).offset(8)
            make.bottom.equalTo(mainMenuView.snp.top).offset(-25)
            make.left.right.equalToSuperview()
        }
    }
    
    func activateConstraintsOfMainMenu() {
        mainMenuView.snp.makeConstraints { (make) in
            make.left.right.equalToSuperview()
            make.height.equalTo(52)
            if #available(iOS 11.0, *) {
                make.bottom.equalTo(safeAreaLayoutGuide.snp.bottom).offset(-20)
            } else {
                // Fallback on earlier versions
                make.bottom.equalToSuperview().offset(-20)
            }
        }
    }
    
    func activateConstraintsOfTextView() {
        msgInputView.snp.makeConstraints { (make) in
            make.top.left.bottom.right.equalToSuperview()
        }
    }
    
    func activateConstraintsOfAudiceneList() {
        audiceneListView.snp.makeConstraints { (make) in
            make.top.left.bottom.right.equalToSuperview()
        }
        
    }
}

extension TRTCChorusRootView: TRTCChorusMainMenuDelegate {
    func menuView(menu: TRTCChorusMainMenuView, shouldClick item: IconTuple) -> Bool {
        if item.type == .mute && !viewModel.isOwner && viewModel.mSelfSeatIndex != -1 {
            //TODO
//            let res = !(viewModel.anchorSeatList[viewModel.mSelfSeatIndex].seatInfo?.mute ?? false)
//            if !res {
//                makeToast(.seatmutedText)
//            }
//            return res
        }
        return true
    }
    
    func menuView(menu: TRTCChorusMainMenuView, click item: IconTuple) -> Bool {
        switch item.type {
        case .message:
            // 消息框
            viewModel.openMessageTextInput()
            break
        case .mute:
            // 麦克风
            return viewModel.muteAction(isMute: item.isSelect)
        case .gift:
            showGiftAlert()
            break
        case .micoff:
            let seatIndex = viewModel.mSelfSeatIndex
            if seatIndex > 0 && seatIndex <= viewModel.anchorSeatList.count {
                viewModel.leaveSeat()
            }
            break
        }
        return false
    }
}

//MARK: - private method
extension TRTCChorusRootView {
    private func showGiftAlert() {
        let alert = TUIGiftPanelView.init()
        alert.parentView = self
        alert.delegate = self
        addSubview(alert)
        
        alert.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        alert.layoutIfNeeded()
        alert.show()
    }
    
    private func refreshRoomInfo() {
        topView.reloadRoomAvatar()
    }
}

//MARK: - TUIGiftPanelViewDelegate
extension TRTCChorusRootView: TUIGiftPanelViewDelegate {
    func show(giftModel: TUIGiftModel) {
        giftAnimator.show(giftInfo: TUIGiftInfo.init(giftModel: giftModel, sendUser: TRTCChorusIMManager.sharedManager().curUserName, sendUserHeadIcon: TRTCChorusIMManager.sharedManager().curUserAvatar))
        viewModel.sendGift(giftId: giftModel.giftId) { [weak self] (code, msg) in
            if code != 0 {
                guard let `self` = self else { return }
                self.makeToast(msg)
            }
        }
    }
}

//MARK: - TRTCChorusViewResponder
extension TRTCChorusRootView: TRTCChorusViewResponder {
    func onShowChorusGifAnimation() {
        chorusSeatsView.showChorusAnimation(isShow: true)
    }
    
    func onHiddenChorusGifAnimation() {
        chorusSeatsView.showChorusAnimation(isShow: false)
    }
    
    func updateLocalNetwork(network: Int) {
        if let quality = TRTCChorusNetworkQuality.init(rawValue: network) {
            chorusSeatsView.updateAnchorView(networkQuality: quality)
        }
    }
    
    func updateRemoteNetwork(network: Int) {
        if let quality = TRTCChorusNetworkQuality.init(rawValue: network) {
            chorusSeatsView.updateChorusView(networkQuality: quality)
        }
    }
    
    func showToast(message: String) {
        makeToast(message)
    }
    
    func showToastActivity(){
        makeToastActivity(.center)
    }
    
    func hiddenToastActivity() {
        hideToastActivity()
    }
    
    func switchView(type: ChorusRoleType) {
        debugPrint("Began switch view")
        switch type {
        case .audience:
            viewModel.userType = .audience
            mainMenuView.audienceType()
            chorusSeatsView.showNetworkView(isShow:false)
        case .anchor:
            viewModel.userType = .anchor
            mainMenuView.anchorType()
            chorusSeatsView.showNetworkView(isShow:true)
        case .chorus:
            viewModel.userType = .chorus
            mainMenuView.chorusType()
            chorusSeatsView.showNetworkView(isShow:true)
        }
        lyricView.checkBtnShouldHidden()
    }
    
    func changeRoom(info: ChorusRoomInfo) {
        topView.reloadRoomInfo(info)
    }
    
    func refreshAnchorInfos() {
        refreshRoomInfo()
        chorusSeatsView.updateSeatView()
    }
    
    func onSeatMute(isMute: Bool) {
        if isMute {
            makeToast(.mutedText, duration: 0.3)
        } else {
            makeToast(.unmutedText, duration: 0.3)
            if viewModel.isSelfMute {
                return;
            }
        }
        var muteModel: IconTuple?
        for model in mainMenuView.dataSource {
            if model.type == .mute {
                muteModel = model
                break
            }
        }
        if let model = muteModel {
            model.isSelect = !isMute
        }
        mainMenuView.changeMixStatus(isMute: isMute)
    }
    
    func onAnchorMute(isMute: Bool) {
        chorusSeatsView.updateSeatView()
    }
    
    func showAlert(info: (title: String, message: String), sureAction: @escaping () -> Void, cancelAction: (() -> Void)?) {
        let alertController = UIAlertController.init(title: info.title, message: info.message, preferredStyle: .alert)
        let sureAlertAction = UIAlertAction.init(title: .acceptText, style: .default) { (action) in
            sureAction()
        }
        let cancelAlertAction = UIAlertAction.init(title: .refuseText, style: .cancel) { (action) in
            cancelAction?()
        }
        alertController.addAction(sureAlertAction)
        alertController.addAction(cancelAlertAction)
        viewModel.showAlert(viewController: alertController, animated: false, completion: nil)
    }
    
    func showActionSheet(actionTitles: [String], actions: @escaping (Int) -> Void) {
        let actionSheet = UIAlertController.init(title: .selectText, message: "", preferredStyle: .actionSheet)
        actionTitles.enumerated().forEach { (item) in
            let index = item.offset
            let title = item.element
            let action = UIAlertAction.init(title: title, style: UIAlertAction.Style.default) { (action) in
                actions(index)
                actionSheet.dismiss(animated: true, completion: nil)
            }
            actionSheet.addAction(action)
        }
        let cancelAction = UIAlertAction.init(title: .cancelText, style: .cancel) { (action) in
            actionSheet.dismiss(animated: true, completion: nil)
        }
        actionSheet.addAction(cancelAction)
        viewModel.showAlert(viewController: actionSheet, animated: true, completion: nil)
    }
    
    func refreshMsgView() {
        tipsView.refreshList()
    }
    
    func msgInput(show: Bool) {
        if show {
            msgInputView.showMsgInput()
        } else {
            msgInputView.hideTextInput()
        }
    }
    
    func audiceneList(show: Bool) {
        if show {
            audiceneListView.show()
        } else {
            audiceneListView.hide()
        }
    }
    
    func audienceListRefresh() {
        audiceneListView.refreshList()
        topView.reloadAudienceList()
    }
    
    func stopPlayBGM() {
        mainMenuView.audienceType()
    }
    
    func recoveryVoiceSetting() {
        
    }
    
    func showAudienceAlert(seat: SeatInfoModel) {
        let audienceList = viewModel.memberAudienceList
        let alert = TRTCChorusAudienceAlert(viewModel: viewModel, seatModel: seat, audienceList: audienceList)
        addSubview(alert)
        alert.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        alert.layoutIfNeeded()
        alert.show()
    }
    
    func showGiftAnimation(giftInfo: TUIGiftInfo) {
        giftAnimator.show(giftInfo: giftInfo)
    }
    
}

//MARK: - internationalization string
fileprivate extension String {
    static let mutedText = ChorusLocalize("Demo.TRTC.Salon.seatmuted")
    static let unmutedText = ChorusLocalize("Demo.TRTC.Salon.seatunmuted")
    static let acceptText = ChorusLocalize("Demo.TRTC.LiveRoom.accept")
    static let refuseText = ChorusLocalize("Demo.TRTC.LiveRoom.refuse")
    static let selectText = ChorusLocalize("Demo.TRTC.Salon.pleaseselect")
    static let cancelText = ChorusLocalize("Demo.TRTC.LiveRoom.cancel")
    static let seatmutedText = ChorusLocalize("Demo.TRTC.Chorus.onseatmuted")
}
