//
//  TUIGiftBulletView.swift
//  TUIChorus
//
//  Created by adams on 2021/6/25.
//
// 礼物播放UI视图控件

import Foundation
import Kingfisher

class TUIGiftBulletView: UIView {
    
    public var giftInfo: TUIGiftInfo? {
        didSet {
            guard let giftInfo = giftInfo else { return }
            if let url = URL.init(string: giftInfo.sendUserHeadIcon) {
                avatarView.kf.setImage(with: .network(url), placeholder: UIImage.init(named: "gift", in: ChorusBundle(), compatibleWith: nil))
            }
            nickNameLabel.text = giftInfo.sendUser
            giftNameLabel.text = "送出了\(giftInfo.giftModel.title)"
            if let url = URL.init(string: giftInfo.giftModel.giftImageUrl) {
                giftIconView.kf.setImage(with: .network(url))
            }
        }
    }
    
    private var animationCompletion: (() -> Void)?
    
    private lazy var contentView: UIView = {
        let view = UIView.init(frame: .zero)
        view.backgroundColor = UIColor.init(white: 0, alpha: 0.5)
        return view
    }()
    
    private lazy var avatarView: UIImageView = {
        let imageView = UIImageView.init(frame: .zero)
        imageView.layer.cornerRadius = 20.0
        imageView.layer.masksToBounds = true
        return imageView
    }()
    
    private lazy var nickNameLabel: UILabel = {
        let label = UILabel.init(frame: .zero)
        label.text = "我"
        label.textColor = .white
        label.font = .systemFont(ofSize: 13.0)
        label.textAlignment = .left
        label.sizeToFit()
        return label
    }()
    
    private lazy var giftNameLabel: UILabel = {
        let label = UILabel.init(frame: .zero)
        label.text = "送出火箭"
        label.textColor = .white
        label.font = .systemFont(ofSize: 13.0)
        label.textAlignment = .left
        label.sizeToFit()
        return label
    }()
    
    private lazy var giftIconView: UIImageView = {
        let imageView = UIImageView.init(frame: .zero)
        imageView.isHidden = true
        return imageView
    }()
    
    private var isViewReady = false
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        contentView.layer.cornerRadius = contentView.bounds.size.height * 0.5
    }
    
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else {
            return
        }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
    }
    
    private func constructViewHierarchy() {
        self.backgroundColor = .clear
        addSubview(contentView)
        contentView.addSubview(avatarView)
        contentView.addSubview(nickNameLabel)
        contentView.addSubview(giftNameLabel)
        contentView.addSubview(giftIconView)
    }
    
    private func activateConstraints() {
        contentView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        avatarView.snp.makeConstraints { make in
            make.size.equalTo(CGSize.init(width: 40, height: 40))
            make.left.equalToSuperview().offset(5)
            make.top.equalToSuperview().offset(5)
            make.bottom.equalToSuperview().offset(-5)
        }
        
        nickNameLabel.snp.makeConstraints { make in
            make.top.equalTo(self.avatarView.snp.top)
            make.right.equalTo(self.giftIconView.snp.left).offset(-5)
            make.left.equalTo(self.avatarView.snp.right).offset(10)
        }
        
        giftNameLabel.snp.makeConstraints { make in
            make.bottom.equalTo(self.avatarView.snp.bottom)
            make.left.equalTo(self.nickNameLabel.snp.left)
            make.right.equalTo(self.giftIconView.snp.left).offset(-5)
        }
        
        giftIconView.snp.makeConstraints { make in
            make.size.equalTo(CGSize.init(width: 40, height: 40))
            make.centerY.equalToSuperview()
            make.right.equalToSuperview().offset(-5)
        }
    }
    
    private func bindInteraction() {
        
    }
    
    deinit {
        print("deinit \(type(of: self))")
    }
    
}

//MARK: 动画
extension TUIGiftBulletView {
    
    public func play(completion: (()->Void)?) {
        layoutIfNeeded()
        animationCompletion = completion
        doAnimationContentEnter()
    }
    
    private func doAnimationContentEnter() {
        let width = contentView.bounds.width
        let contentAnimation = CAKeyframeAnimation.init(keyPath: "position.x")
        contentAnimation.values = [-width * 0.5,  width * 0.5 + 20,  width * 0.5]
        contentAnimation.duration = 0.25
        let animationDelegate = TUIGiftBulletAnimation.init()
        animationDelegate.delegate = self
        contentAnimation.delegate = animationDelegate
        contentAnimation.isRemovedOnCompletion = false
        contentView.layer.add(contentAnimation, forKey: "contentAnimationShow")
    }
    
    private func doAnimationGiftIconEnter() {
        giftIconView.isHidden = false
        let width = contentView.bounds.width
        let giftAnimation = CAKeyframeAnimation.init(keyPath: "position.x")
        giftAnimation.values = [-width * 0.5,  width - 0.5 * giftIconView.bounds.width]
        giftAnimation.duration = 0.25
        let animationDelegate = TUIGiftBulletAnimation.init()
        animationDelegate.delegate = self
        giftAnimation.delegate = animationDelegate
        giftAnimation.isRemovedOnCompletion = false
        giftIconView.layer.add(giftAnimation, forKey: "giftIconAnimation")
    }
    
    private func doAnimationContentDismiss() {
        let contentPositionAnimation = CAKeyframeAnimation.init(keyPath: "position.y")
        contentPositionAnimation.values = [0,-contentView.bounds.height]
        contentPositionAnimation.duration = 0.25
        
        let contentAlphaAnimation = CABasicAnimation.init(keyPath: "opacity")
        contentAlphaAnimation.fromValue = 1.0
        contentAlphaAnimation.toValue  = 0.0
        contentAlphaAnimation.duration = 0.25
        
        let contentHiddenAnimation = CAAnimationGroup.init()
        contentHiddenAnimation.animations = [contentPositionAnimation, contentAlphaAnimation]
        let animationDelegate = TUIGiftBulletAnimation.init()
        animationDelegate.delegate = self
        contentHiddenAnimation.delegate = animationDelegate
        contentHiddenAnimation.isRemovedOnCompletion = false
        contentView.layer.add(contentHiddenAnimation, forKey: "contentViewHiddenAnimation")
    }
    
}



//MARK: 动画 CAAnimationDelegate
extension TUIGiftBulletView: TUIGiftBulletAnimationDelegate {
    func animationDidStop(_ anim: CAAnimation, finished flag: Bool) {
        if contentView.layer.animation(forKey: "contentAnimationShow") == anim {
            if flag {
                contentView.layer.removeAllAnimations()
                doAnimationGiftIconEnter()
            }
        } else if giftIconView.layer.animation(forKey: "giftIconAnimation") == anim {
            if flag {
                giftIconView.layer.removeAllAnimations()
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    self.doAnimationContentDismiss()
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                        self.contentView.isHidden = true
                    }
                }
            }
        } else if contentView.layer.animation(forKey: "contentViewHiddenAnimation") == anim {
            if flag {
                contentView.layer.removeAllAnimations()
                if let animationCompletion = animationCompletion {
                    animationCompletion()
                }
            }
        }
    }
}

protocol TUIGiftBulletAnimationDelegate: NSObject {
    
    func animationDidStop(_ anim: CAAnimation, finished flag: Bool)
    
}

class TUIGiftBulletAnimation: NSObject, CAAnimationDelegate {
    
    weak var delegate: TUIGiftBulletAnimationDelegate?
    
    func animationDidStop(_ anim: CAAnimation, finished flag: Bool) {
        if let delegate = delegate {
            delegate.animationDidStop(anim, finished: flag)
        }
    }
    
    deinit {
        debugPrint("deinit \(type(of: self))")
    }
}



