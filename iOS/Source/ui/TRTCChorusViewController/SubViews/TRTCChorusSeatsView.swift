//
//  TRTCChorusSeatsView.swift
//  TUIChorus
//
//  Created by adams on 2021/8/5.
//

import UIKit
import Kingfisher

enum TRTCChorusNetworkStatus {
    case unknown            ///未定义
    case ethernetOrWiFi     ///以太网或WiFi
    case cellular           ///蜂窝网
}

enum TRTCChorusNetworkQuality: Int {
    case unknown = 0        ///未定义
    case excellent = 1      ///当前网络非常好
    case good = 2           ///当前网络比较好
    case poor = 3           ///当前网络一般
    case bad = 4            ///当前网络较差
    case Vbad = 5           ///当前网络很差
    case down = 6           ///当前网络不满足 TRTC 的最低要求
}

class TRTCChorusSeatsView: UIView {
    
    private var isViewReady = false
    private let viewModel: TRTCChorusViewModel
    
    lazy var anchorSeatView: TRTCChorusSeatView = {
        let view = TRTCChorusSeatView.init(frame: .zero)
        view.tag = 2000
        return view
    }()
    
    lazy var chorusSeatView: TRTCChorusSeatView = {
        let view = TRTCChorusSeatView.init(frame: .zero)
        view.tag = 2001
        return view
    }()
    
    lazy var anchorNetworkQualityView: TRTCChorusNetworkQualityView = {
        let view = TRTCChorusNetworkQualityView.init(frame: .zero)
        return view
    }()
    
    lazy var chorusNetworkQualityView: TRTCChorusNetworkQualityView = {
        let view = TRTCChorusNetworkQualityView.init(frame: .zero)
        view.update(networkQuality: .unknown)
        return view
    }()
    
    lazy var containerView: UIView = {
        let view = UIView.init(frame: .zero)
        view.backgroundColor = UIColor.clear
        return view
    }()
    
    lazy var chorusGifImageView: UIImageView = {
        let imageView = UIImageView.init()
        imageView.contentMode = .scaleToFill
        imageView.isHidden = true
        return imageView
    }()
    
    init(frame: CGRect, viewModel: TRTCChorusViewModel) {
        self.viewModel = viewModel
        super.init(frame: frame)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
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
        DispatchQueue.main.async {
            let path = ChorusBundle().path(forResource: "ChorusPlaying", ofType: "gif") ?? ""
            let url = URL.init(fileURLWithPath: path)
            let provider = LocalFileImageDataProvider.init(fileURL: url)
            self.chorusGifImageView.kf.setImage(with: provider)
        }
    }
    
    private func constructViewHierarchy() {
        addSubview(chorusGifImageView)
        addSubview(containerView)
        containerView.addSubview(anchorSeatView)
        containerView.addSubview(chorusSeatView)
        
        addSubview(anchorNetworkQualityView)
        addSubview(chorusNetworkQualityView)
        showNetworkView(isShow:viewModel.userType == .audience)
    }
    
    private func activateConstraints() {
        containerView.snp.makeConstraints { make in
            make.center.equalToSuperview()
        }
        
        anchorSeatView.snp.makeConstraints { make in
            make.left.top.bottom.equalToSuperview()
            make.size.equalTo(CGSize.init(width: 110, height: 110))
        }
        
        chorusSeatView.snp.makeConstraints { make in
            make.top.bottom.right.equalToSuperview()
            make.left.equalTo(anchorSeatView.snp.right).offset(44)
            make.size.equalTo(anchorSeatView.snp.size)
        }
        
        let imageSize = UIImage.init(contentsOfFile:  ChorusBundle().path(forResource: "ChorusPlaying", ofType: "gif")!)!.size
        let imageScale = imageSize.width / imageSize.height
        
        chorusGifImageView.snp.makeConstraints { make in
            make.right.equalTo(chorusSeatView.avatarImageView.snp.left).offset(3)
            make.left.equalTo(anchorSeatView.avatarImageView.snp.right)
            make.centerY.equalTo(chorusSeatView.avatarImageView.snp.centerY)
            make.height.equalTo(chorusGifImageView.snp.width).dividedBy(imageScale)
        }
        
        anchorNetworkQualityView.snp.makeConstraints { make in
            make.right.equalTo(chorusNetworkQualityView.snp.left).offset(-10)
            make.bottom.equalToSuperview()
            make.width.equalTo(80)
            make.top.equalTo(anchorSeatView.snp.bottom)
        }
        
        chorusNetworkQualityView.snp.makeConstraints { make in
            make.right.equalToSuperview().offset(-20)
            make.bottom.equalToSuperview()
            make.width.equalTo(80)
            make.top.equalTo(anchorSeatView.snp.bottom)
        }
    }
    
    private func bindInteraction() {
        let anchorTap = UITapGestureRecognizer.init(target: self, action: #selector(anchorTapClick(sender:)))
        anchorSeatView.addGestureRecognizer(anchorTap)
        
        let chorusTap = UITapGestureRecognizer.init(target: self, action: #selector(chorusTapClick(sender:)))
        chorusSeatView.addGestureRecognizer(chorusTap)
    }
   
}

extension TRTCChorusSeatsView {
    
    public func showChorusAnimation(isShow: Bool) {
        self.chorusGifImageView.isHidden = !isShow
    }
    
    public func updateSeatView() {
        viewModel.anchorSeatList.enumerated().forEach { item in
            let index = item.offset
            let seatInfoModel = item.element
            guard let seatView = self.viewWithTag(index + 2000) as? TRTCChorusSeatView else {
                debugPrint("seat view is nil")
                return
            }
            seatView.setSeatInfo(model: seatInfoModel, seatIndex: index)
        }
    }
    
    public func showNetworkView(isShow: Bool) {
        anchorNetworkQualityView.isHidden = !isShow
        chorusNetworkQualityView.isHidden = !isShow
        if (!isShow) {
            updateAnchorView(networkQuality: .unknown)
            updateChorusView(networkQuality: .unknown)
        }
    }
    
    public func updateAnchorView(networkQuality: TRTCChorusNetworkQuality) {
        anchorNetworkQualityView.isHidden = viewModel.userType == .audience
        anchorNetworkQualityView.update(networkQuality: networkQuality)
    }
    
    public func updateAnchorView(networkStatus: TRTCChorusNetworkStatus) {
        anchorNetworkQualityView.isHidden = viewModel.userType == .audience
        anchorNetworkQualityView.update(networkStatus: networkStatus)
    }
    
    public func updateChorusView(networkQuality: TRTCChorusNetworkQuality) {
        chorusNetworkQualityView.isHidden = viewModel.userType == .audience
        chorusNetworkQualityView.update(networkQuality: networkQuality)
    }
    
    public func updateChorusView(networkStatus: TRTCChorusNetworkStatus) {
        chorusNetworkQualityView.isHidden = viewModel.userType == .audience
        chorusNetworkQualityView.update(networkStatus: networkStatus)
    }
}

extension TRTCChorusSeatsView {
    @objc private func anchorTapClick(sender: UITapGestureRecognizer) {
        guard let seatInfoModel = viewModel.anchorSeatList.first else {
            debugPrint("seatInfoModel is nil")
            return
        }
        seatInfoModel.action?(0)
    }
    
    @objc private func chorusTapClick(sender: UITapGestureRecognizer) {
        guard let seatInfoModel = viewModel.anchorSeatList.last else {
            debugPrint("seatInfoModel is nil")
            return
        }
        seatInfoModel.action?(1)
    }
}

class TRTCChorusNetworkQualityView: UIView {
    
    private var networkImageHeadName: String = "Wifi-signal"
    
    private var networkQuality: TRTCChorusNetworkQuality = .unknown
    
    private lazy var networkQualityImageView: UIImageView = {
        let imageView = UIImageView.init(frame: .zero)
        return imageView
    }()
    
    private lazy var networkQualityTitleLabel: UILabel = {
        let titleLabel = UILabel.init(frame: .zero)
        titleLabel.textColor = UIColor.green
        titleLabel.adjustsFontSizeToFitWidth = true
        titleLabel.textAlignment = .center
        return titleLabel
    }()
    
    private var isViewReady = false
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
        self.backgroundColor = UIColor.systemGray
        addSubview(networkQualityImageView)
        addSubview(networkQualityTitleLabel)
    }
    
    private func activateConstraints() {
        networkQualityImageView.snp.makeConstraints { make in
            make.left.equalToSuperview().offset(5)
            make.size.equalTo(CGSize.init(width: 20, height: 20))
            make.centerY.equalToSuperview()
        }
        
        networkQualityTitleLabel.snp.makeConstraints { make in
            make.right.equalToSuperview().offset(-5)
            make.centerY.equalToSuperview()
            make.left.equalTo(networkQualityImageView.snp.right).offset(5)
        }
    }
    
    private func bindInteraction() {
        
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        layer.cornerRadius = 4
        layer.masksToBounds = true
    }
}

extension TRTCChorusNetworkQualityView {
    public func update(networkStatus: TRTCChorusNetworkStatus) {
        switch networkStatus {
            case .unknown, .cellular:
                networkImageHeadName = "Cellular-signal"
            case .ethernetOrWiFi:
                networkImageHeadName = "Wifi-signal"
        }
        networkQualityImageView.image = UIImage.init(named: networkImageHeadName + getNetworkIcon(), in: ChorusBundle(), compatibleWith: nil)
    }
    
    public func update(networkQuality: TRTCChorusNetworkQuality) {
        self.networkQuality = networkQuality
        self.networkQualityTitleLabel.text = getNetworkText()
        self.networkQualityTitleLabel.textColor = getNetworkTextColor()
        if getNetworkIcon() == "04" {
            self.networkQualityImageView.image = UIImage.init(named: "lock", in: ChorusBundle(), compatibleWith: nil)
        } else {
            self.networkQualityImageView.image = UIImage.init(named: networkImageHeadName + getNetworkIcon(), in: ChorusBundle(), compatibleWith: nil)
        }
    }
    
    private func getNetworkText() -> String {
        switch networkQuality {
        case .excellent, .good:
            return .normalText
        case .poor:
            return .generalText
        case .bad, .Vbad:
            return .poorText
        case .unknown, .down:
            return .unknownText
        }
    }
    
    private func getNetworkIcon() -> String {
        switch networkQuality {
        case .excellent, .good:
            return "01"
        case .poor:
            return "02"
        case .bad, .Vbad:
            return "03"
        case .unknown, .down:
            return "04"
        }
    }
    
    private func getNetworkTextColor() -> UIColor {
        switch networkQuality {
        case .excellent, .good:
            return .green
        case .poor:
            return .yellow
        case .bad, .Vbad:
            return .red
        case .unknown, .down:
            return .gray
        }
    }
}


/// MARK: - internationalization string
fileprivate extension String {
    static let normalText = ChorusLocalize("Demo.TRTC.Chorus.normal")
    static let generalText = ChorusLocalize("Demo.TRTC.Chorus.general")
    static let poorText = ChorusLocalize("Demo.TRTC.Chorus.poor")
    static let unknownText = ChorusLocalize("Demo.TRTC.Chorus.unknown")
}

