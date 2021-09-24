//
//  TUIGiftAnimator.swift
//  TUIChorus
//
//  Created by adams on 2021/6/25.
//
// 礼物播放动画管理类 目前不支持大礼物

import Foundation

public class TUIGiftAnimator: NSObject {
    
    private let kMaxBulletGift = 3
    
    private lazy var bigGiftManager: TUIGiftAnimationManager = {
       let manager = TUIGiftAnimationManager.init()
        manager.delegate = self
        return manager
    }()
    
    private weak var animationContainerView: UIView?
    
    private var bulletGiftCount: NSInteger
    
    init(animationContainerView: UIView?) {
        self.animationContainerView = animationContainerView
        self.bulletGiftCount = 0
        super.init()
        clearBulletGift()
    }
    
    private func clearBulletGift() {
        guard let containerView = animationContainerView else { return }
        for subView in containerView.subviews where subView is TUIGiftBulletView {
            debugPrint("\(type(of: subView))")
            subView.removeFromSuperview()
        }
    }
    
    func show(giftInfo: TUIGiftInfo) {
        /**
         将礼物播放动画事件加入到礼物动画播放管理器中(内含播放队列)，管理器会控制代理进行动画的播放，可通过completion来实现动画按顺序拉取播放
         */
        // 弹幕礼物
        playBulletGiftAnimation(giftInfo: giftInfo, completion: nil)
        if giftInfo.giftModel.type == 1 {
            // 大礼物
//            bigGiftManager.onRecevie(giftInfo: giftInfo)
        }
    }
    
    // 播放大礼物特效
    private func playBigGiftAnimation(giftInfo: TUIGiftInfo, completion: (() -> Void)?) {
        
    }
    
    // 播放小礼物特效
    private func playBulletGiftAnimation(giftInfo: TUIGiftInfo, completion: (() -> Void)?) {
        let giftBulletAnimation = TUIGiftBulletView.init(frame: .zero)
        giftBulletAnimation.giftInfo = giftInfo
        guard let canvasView = animationContainerView else { return }
        canvasView.addSubview(giftBulletAnimation)
        if bulletGiftCount <= 0 {
            bulletGiftCount = 0
        }
        bulletGiftCount += 1
        giftBulletAnimation.snp.makeConstraints { make in
            make.left.equalTo(canvasView.snp.left).offset(10)
            make.bottom.equalTo(canvasView.snp.bottom).offset(-270)
        }
        canvasView.layoutIfNeeded()
        giftBulletAnimation.play {
            giftBulletAnimation.isHidden = true
            if let completion = completion {
                completion()
            }
        }
        
        var i = 0
        var first: TUIGiftBulletView? = nil
        for giftView in canvasView.subviews where giftView is TUIGiftBulletView {
            if i == 0 {
                first = giftView as? TUIGiftBulletView
            }
            
            giftView.snp.makeConstraints { make in
                make.bottom.equalTo(canvasView.snp.bottom).offset(-270 - (bulletGiftCount - 1 - i) * 55)
            }
            i += 1
        }
        
        if bulletGiftCount > kMaxBulletGift && first != nil {
            first?.removeFromSuperview()
            bulletGiftCount -= 1
        }
        
        UIView.animate(withDuration: 0.25) {
            canvasView.layoutIfNeeded()
        }
    }
    
    deinit {
        print("deinit \(type(of: self))")
    }
}

extension TUIGiftAnimator: TUIGiftAnimationManagerDelegate {
    public func giftAnimationManager(manager: TUIGiftAnimationManager, giftInfo: TUIGiftInfo?, completion: @escaping () -> Void) {
        if manager === bigGiftManager {
            if let giftInfo = giftInfo {
                playBigGiftAnimation(giftInfo: giftInfo) {
                    completion()
                }
            }
        }
    }
}
