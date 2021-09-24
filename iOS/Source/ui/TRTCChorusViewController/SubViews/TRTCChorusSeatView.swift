//
//  TRTCChorusSeatView.swift
//  TUIChorus
//
//  Created by adams on 2021/8/4.
//  Copyright © 2021 tencent. All rights reserved.
//
import UIKit
import TXAppBasic
import SnapKit

// 设置字号和透明度的
enum TRTCSeatState {
    case cellSeatEmpty
    case cellSeatFull
    case masterSeatEmpty
    case masterSeatFull
}

// 需要设置合理的高度和宽度获得正常的显示效果(高度越高，name和avatar之间的间距越大)
class TRTCChorusSeatView: UIView {
    private var isViewReady: Bool = false
    private var isGetBounds: Bool = false
    
    override init(frame: CGRect = .zero) {
        super.init(frame: frame)
        bindInteraction()
        setupStyle()
    }
    
    required init?(coder: NSCoder) {
        fatalError("can't init this viiew from coder")
    }
    
    deinit {
        TRTCLog.out("seat view deinit")
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        avatarImageView.layer.cornerRadius = avatarImageView.frame.height*0.5
        
        speakView.layer.cornerRadius = speakView.frame.height*0.5
        speakView.layer.borderWidth = 4
        speakView.layer.borderColor = UIColor.init(0x0FA968).cgColor
    }
    let speakView: UIView = {
        let view = UIView.init()
        view.backgroundColor = UIColor.clear
        view.isHidden = true
        return view
    }()
    let avatarImageView: UIImageView = {
        let imageView = UIImageView.init(frame: .zero)
        imageView.contentMode = .scaleAspectFit
        imageView.image = UIImage.init(named: "Chorus_placeholder_avatar", in: ChorusBundle(), compatibleWith: nil)
        imageView.layer.masksToBounds = true
        return imageView
    }()
    
    let muteImageView: UIImageView = {
        let imageView = UIImageView.init(frame: .zero)
        imageView.contentMode = .scaleAspectFit
        imageView.image = UIImage.init(named: "audience_voice_off", in: ChorusBundle(), compatibleWith: nil)
        imageView.isHidden = true
        return imageView
    }()
    
    let nameLabel: UILabel = {
        let label = UILabel.init(frame: .zero)
        label.text = .handsupText
        label.font = UIFont(name: "PingFangSC-Regular", size: 16)
        label.textColor = .white
        label.textAlignment = .center
        label.numberOfLines = 2
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.5
        return label
    }()
    
    // MARK: - 视图生命周期函数
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else {
            return
        }
        isViewReady = true
        constructViewHierarchy() // 视图层级布局
        activateConstraints() // 生成约束（此时有可能拿不到父视图正确的frame）
    }

    func setupStyle() {
        backgroundColor = .clear
    }
    
    func constructViewHierarchy() {
        /// 此方法内只做add子视图操作
        addSubview(avatarImageView)
        addSubview(muteImageView)
        addSubview(nameLabel)
        avatarImageView.addSubview(speakView)
    }

    func activateConstraints() {
        /// 此方法内只给子视图做布局,使用:AutoLayout布局
        avatarImageView.snp.makeConstraints { (make) in
            make.top.centerX.equalToSuperview()
            make.width.equalTo(70)
            make.height.equalTo(avatarImageView.snp.width)
        }
        muteImageView.snp.makeConstraints { (make) in
            make.trailing.bottom.equalTo(avatarImageView)
        }
        nameLabel.snp.makeConstraints { (make) in
            make.leading.trailing.bottom.equalToSuperview()
            make.top.equalTo(avatarImageView.snp.bottom).offset(4)
            make.width.lessThanOrEqualTo(120)
        }
        speakView.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
    }

    func bindInteraction() {
        /// 此方法负责做viewModel和视图的绑定操作
    }
    
    func isMute(userId: String, map: [String:Bool]) -> Bool {
        if map.keys.contains(userId) {
            return map[userId]!
        }
        return true
    }
    
    func setSeatInfo(model: SeatInfoModel, seatIndex: Int) {
        
        if model.isUsed {
            // 有人
            if let userSeatInfo = model.seatUser {
                let placeholder = UIImage.init(named: "avatar2_100", in: ChorusBundle(), compatibleWith: nil)
                let avatarStr = TRTCChorusIMManager.sharedManager().checkAvatar(userSeatInfo.userAvatar)
                if let avatarURL = URL.init(string: avatarStr) {
                    avatarImageView.kf.setImage(with: avatarURL, placeholder: placeholder)
                } else {
                    avatarImageView.image = placeholder
                }
                nameLabel.text = userSeatInfo.userName
            }
        } else {
            // 无人
            avatarImageView.image = UIImage.init(named: "seatDefault", in: ChorusBundle(), compatibleWith: nil)
            nameLabel.text = LocalizeReplaceXX(.seatIndexText, "\(seatIndex + 1)")
        }
        
        if model.isClosed {
            // close 状态
            avatarImageView.image = UIImage.init(named: "room_lockseat", in: ChorusBundle(), compatibleWith: nil)
            speakView.isHidden = true
            muteImageView.isHidden = true
            return
        }
        
        muteImageView.isHidden = true
        
        if (model.isTalking) {
            speakView.isHidden = false
        } else {
            speakView.isHidden = true
        }
    }
}

/// MARK: - internationalization string
fileprivate extension String {
    static let handsupText = ChorusLocalize("Demo.TRTC.Chorus.presshandsup")
    static let lockedText = ChorusLocalize("Demo.TRTC.Chorus.islocked")
    static let inviteHandsupText = ChorusLocalize("Demo.TRTC.Chorus.invitehandsup")
    static let seatIndexText = ChorusLocalize("Demo.TRTC.Chorus.xxmic")
}



