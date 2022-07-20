//
//  TRTCChorusFloatingWindow.swift
//  TUIChorus
//
//  Created by adams on 2021/8/2.
//  Copyright © 2022 Tencent. All rights reserved.

import Foundation
import TXAppBasic

fileprivate protocol ChorusFloatingWindowDelegate: NSObject {
    func onChangedFrame()
    func onClickView()
    func onMuteBtnClick()
    func onCloseBtnClick()
}

public class TRTCChorusFloatingWindowManager: NSObject {
    
    // MARK: - Public Interface
    public static func shared() -> TRTCChorusFloatingWindowManager { staticInstance }
    
    public var enableFloatingWindow: Bool = true
    
    public var windowIsShowing: Bool {
        get {
            return isShowing
        }
    }
    
    public func closeWindowAndExitRoom(completion: (() -> ())? = nil) {
        guard let viewModel = sourceVC?.viewModel else {
            return
        }
        viewModel.exitRoom { [weak self] in
            guard let `self` = self else { return }
            UIView.animate(withDuration: 0.3) {
                self.presentWindow?.alpha = 0
            } completion: { (finish) in
                self.sourceVC = nil
                if let action = completion {
                    action()
                }
            }
        }
    }
    
    public var currentRoomID: Int {
        get {
            guard let viewModel = sourceVC?.viewModel else {
                return -1
            }
            return viewModel.roomInfo.roomID
        }
    }
    
    public func hide(vc: TRTCChorusViewController) {
        sourceVC = vc
        guard let vc = sourceVC else { return }
        guard let snapshot = vc.view.snapshotView(afterScreenUpdates: false) else { return }
        snapshotWindow = makeSnapshotWindow(snapshot: snapshot)
        snapshotWindow?.frame.size = snapshot.frame.size
        snapshotWindow?.frame.origin = .zero
        snapshotWindow?.makeKeyAndVisible()
        
        if let url = URL(string: TRTCChorusIMManager.sharedManager().checkBgImage(vc.viewModel.roomInfo.coverUrl)) {
            presentWindow?.thumbnailImageView.kf.setImage(with: .network(url))
        }
        
        UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseOut) {
            self.snapshotWindow?.frame = self.windowFrame
            self.snapshotWindow?.alpha = 0
            self.presentWindow?.containerView.alpha = 1
        } completion: { (finish) in
            self.snapshotWindow = nil
        }
        if let nav = vc.navigationController {
            presentVC = nav
            nav.popViewController(animated: false)
        }
        else {
            presentVC = vc
            vc.dismiss(animated: false, completion: nil)
        }
        isShowing = true
    }
    
    public func show(fromVC: UIViewController) {
        guard let vc = sourceVC else {
            return
        }
        guard let snapshot = vc.view.snapshotView(afterScreenUpdates: true) else {
            debugPrint("___snap shot error")
            return
        }
        guard let window = presentWindow else {
            return
        }
        snapshotWindow = makeSnapshotWindow(snapshot: snapshot)
        snapshotWindow?.frame = window.frame
        snapshotWindow?.alpha = 0
        snapshotWindow?.makeKeyAndVisible()
        
        UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseIn) {
            self.snapshotWindow?.alpha = 1
            self.snapshotWindow?.frame = UIScreen.main.bounds
            self.presentWindow?.containerView.alpha = 0
        } completion: { (finish) in
            guard let sourceVC = self.sourceVC else {
                return
            }
            if let fromVC = fromVC as? UINavigationController {
                sourceVC.hidesBottomBarWhenPushed = true
                fromVC.pushViewController(sourceVC, animated: false)
            }
            else {
                fromVC.present(sourceVC, animated: false, completion: nil)
            }
            
//            if window.muteBtn.isSelected {
//                window.muteBtn.sendActions(for: .touchUpInside)
//            }
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                self.snapshotWindow?.alpha = 0
                self.snapshotWindow = nil
                self.sourceVC = nil
                self.isShowing = false
            }
        }
    }
    
    
    // MARK: - Private
    private static let staticInstance: TRTCChorusFloatingWindowManager = TRTCChorusFloatingWindowManager()
    private override init() {}
    
    private var isShowing: Bool = false
    
    private var windowFrame: CGRect = CGRect(x: 0, y: ScreenHeight * 0.5, width: 134, height: 50)
    
    private var sourceVC: TRTCChorusViewController? {
        didSet {
            guard let sourceVC = sourceVC else {
                snapshotWindow = nil
                presentWindow = nil
                isShowing = false
                return
            }
            presentWindow = TRTCChorusFloatingWindow(frame: sourceVC.view.bounds)
        }
    }
    
    private var presentWindow: TRTCChorusFloatingWindow? {
        didSet {
            presentWindow?.frame = windowFrame
            presentWindow?.makeKeyAndVisible()
            presentWindow?.delegate = self
        }
    }
    
    private var presentVC: UIViewController?
    
    private var snapshotWindow: UIWindow?
    
    private func makeSnapshotWindow(snapshot: UIView) -> UIWindow {
        let window = UIWindow(frame: .zero)
        window.clipsToBounds = true
        window.windowLevel = .statusBar - 1
        snapshot.frame.origin = .zero
        window.addSubview(snapshot)
        return window
    }
}

extension TRTCChorusFloatingWindowManager: ChorusFloatingWindowDelegate {
    func onMuteBtnClick() {
//        guard let viewModel = sourceVC?.viewModel, let window = presentWindow else {
//            return
//        }
//        viewModel.spechAction(isMute: window.muteBtn.isSelected)
//        var superView: UIView?
//        for win in UIApplication.shared.windows {
//            if win != window && win != snapshotWindow {
//                superView = win
//                break
//            }
//        }
//        superView?.makeToast(window.muteBtn.isSelected ? .mutedText : .unmutedText)
    }
    
    func onCloseBtnClick() {
        closeWindowAndExitRoom()
    }
    
    func onChangedFrame() {
        guard let window = presentWindow else {
            return
        }
        snapshotWindow?.frame = window.frame
        windowFrame = window.frame
    }
    
    func onClickView() {
        guard let vc = presentVC else {
            return
        }
        show(fromVC: vc)
    }
}

fileprivate class TRTCChorusFloatingWindow: UIWindow {
    
    fileprivate weak var delegate: ChorusFloatingWindowDelegate?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        windowLevel = .statusBar - 2
        
        addSubview(containerView)
        containerView.frame = CGRect(x: 0, y: 0, width: 100, height: 50)
        
        containerView.addSubview(blurBgView)
        blurBgView.frame = containerView.bounds
        
        containerView.addSubview(thumbnailImageView)
        thumbnailImageView.frame.size = CGSize(width: 42, height: 42)
        thumbnailImageView.center = CGPoint(x: 25, y: 25)
        
//        containerView.addSubview(muteBtn)
//        muteBtn.sizeToFit()
//        muteBtn.center = CGPoint(x: 50 + 21, y: 25)
        
        containerView.addSubview(closeBtn)
        closeBtn.sizeToFit()
        closeBtn.center = CGPoint(x: 50 + 21 , y: 25)
        
        
        let tap = UITapGestureRecognizer(target: self, action: #selector(viewDidClick(tap:)))
        containerView.addGestureRecognizer(tap)
        let pan = UIPanGestureRecognizer(target: self, action: #selector(viewDidDrag(pan:)))
        containerView.addGestureRecognizer(pan)
        tap.require(toFail: pan)
        
//        muteBtn.addTarget(self, action: #selector(muteBtnClick), for: .touchUpInside)
        closeBtn.addTarget(self, action: #selector(closeBtnClick), for: .touchUpInside)
        
        containerView.roundedRect(rect: containerView.bounds, byRoundingCorners: [.topRight, .bottomRight], cornerRadii: CGSize(width: 25, height: 25))
    }
    
//    @objc func muteBtnClick() {
//        muteBtn.isSelected = !muteBtn.isSelected
//        self.delegate?.onMuteBtnClick()
//    }
    
    @objc func closeBtnClick() {
        self.delegate?.onCloseBtnClick()
    }
    
    @objc func viewDidClick(tap: UITapGestureRecognizer) {
        self.delegate?.onClickView()
    }
    
    private var beganPoint: CGPoint?
    
    @objc func viewDidDrag(pan: UIPanGestureRecognizer) {
        switch pan.state {
        case .began:
            beganPoint = pan.location(in: self)
        case .changed:
            guard let beganPoint = beganPoint else {
                return
            }
            let point = pan.location(in: self)
            let offsetX = point.x - beganPoint.x
            let offsetY = point.y - beganPoint.y
            let coefficient: CGFloat = 1.01
            let origin = self.frame.origin
            self.frame.origin = CGPoint(x: origin.x + offsetX * coefficient, y: origin.y + offsetY * coefficient)
        case .cancelled, .ended:
            UIView.animate(withDuration: 0.2) {
                self.frame.origin.x = 0
            } completion: { (finish) in
                self.delegate?.onChangedFrame()
            }
        default:
            break
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    lazy var containerView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = UIColor.init(white: 1, alpha: 0.6)
        view.alpha = 0
        return view
    }()
    
    lazy var thumbnailImageView: UIImageView = {
        let imageView = UIImageView(frame: .zero)
        imageView.clipsToBounds = true
        imageView.layer.cornerRadius = 6
        return imageView
    }()
    
//    lazy var muteBtn: UIButton = {
//        let btn = UIButton(type: .custom)
//        btn.setImage(UIImage(named: "volume", in: ChorusBundle(), compatibleWith: nil), for: .normal)
//        btn.setImage(UIImage(named: "volume_off", in: ChorusBundle(), compatibleWith: nil), for: .selected)
//        return btn
//    }()
    
    lazy var closeBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.setImage(UIImage(named: "close", in: ChorusBundle(), compatibleWith: nil), for: .normal)
        return btn
    }()
    
    lazy var blurBgView: UIVisualEffectView = {
        let blur = UIBlurEffect(style: .extraLight)
        let view = UIVisualEffectView(effect: blur)
        return view
    }()
}

fileprivate extension String {
    static let exitText = ChorusLocalize("Demo.TRTC.Chorus.exit")
    static let sureToExitText = ChorusLocalize("Demo.TRTC.Chorus.isvoicingandsuretoexit")
    static let acceptText = ChorusLocalize("Demo.TRTC.LiveRoom.accept")
    static let refuseText = ChorusLocalize("Demo.TRTC.LiveRoom.refuse")
    static let mutedText = ChorusLocalize("Demo.TRTC.Chorus.ismuted")
    static let unmutedText = ChorusLocalize("Demo.TRTC.Chorus.isunmuted")
}
