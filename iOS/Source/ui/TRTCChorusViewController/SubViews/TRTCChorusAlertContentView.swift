//
//  TRTCChorusAlertContentView.swift
//  TUIChorus
//
//  Created by adams on 2021/7/15.
//

import UIKit
import TXAppBasic

class TRTCChorusAlertContentView: UIView {
    lazy var bgView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = .black
        view.alpha = 0
        return view
    }()
    lazy var blurView: UIVisualEffectView = {
        let effect = UIBlurEffect(style: .dark)
        let view = UIVisualEffectView(effect: effect)
        return view
    }()
    lazy var contentView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = .clear
        return view
    }()
    
    lazy var titleLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.textColor = .white
        label.font = UIFont(name: "PingFangSC-Medium", size: 24)
        return label
    }()
    
    let viewModel: TRTCChorusViewModel
    
    public var willDismiss: (()->())?
    public var didDismiss: (()->())?
    
    public init(frame: CGRect = .zero, viewModel: TRTCChorusViewModel) {
        self.viewModel = viewModel
        super.init(frame: frame)
        contentView.transform = CGAffineTransform(translationX: 0, y: ScreenHeight)
        alpha = 0
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
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
    
    public func show() {
        UIView.animate(withDuration: 0.3) {
            self.alpha = 1
            self.contentView.transform = .identity
        }
    }
    
    public func dismiss() {
        if let action = willDismiss {
            action()
        }
        UIView.animate(withDuration: 0.3) {
            self.alpha = 0
            self.contentView.transform = CGAffineTransform(translationX: 0, y: ScreenHeight)
        } completion: { (finish) in
            if let action = self.didDismiss {
                action()
            }
            self.removeFromSuperview()
        }
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let point = touches.first?.location(in: self) else {
            return
        }
        if !contentView.frame.contains(point) {
            dismiss()
        }
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        contentView.roundedRect(rect: contentView.bounds, byRoundingCorners: [.topLeft, .topRight], cornerRadii: CGSize(width: 12, height: 12))
    }
    
    func constructViewHierarchy() {
        addSubview(bgView)
        addSubview(contentView)
        contentView.addSubview(blurView)
        contentView.addSubview(titleLabel)
    }
    func activateConstraints() {
        bgView.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        contentView.snp.makeConstraints { (make) in
            make.leading.trailing.bottom.equalToSuperview()
        }
        blurView.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        titleLabel.snp.makeConstraints { (make) in
            make.leading.equalToSuperview().offset(20)
            make.top.equalToSuperview().offset(32)
        }
    }
    func bindInteraction() {
        
    }
}

// MARK: - Audience View
class TRTCChorusAudienceAlert: TRTCChorusAlertContentView {
    
    public var unlockBtnDidClick : ((_ selected: Bool)->())?
    
    lazy var unlockBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.setImage(UIImage(named: "lock", in: ChorusBundle(), compatibleWith: nil), for: .normal)
        btn.setImage(UIImage(named: "unlock", in: ChorusBundle(), compatibleWith: nil), for: .selected)
        btn.setTitle(.lockText, for: .normal)
        btn.setTitle(.unlockText, for: .selected)
        btn.titleLabel?.font = UIFont(name: "PingFangSC-Medium", size: 14)
        btn.backgroundColor = UIColor(hex: "F4F5F9")
        btn.setTitleColor(UIColor(hex: "333333"), for: .normal)
        btn.setTitleColor(UIColor(hex: "333333"), for: .selected)
        btn.imageEdgeInsets = UIEdgeInsets(top: 0, left: -2, bottom: 0, right: 2)
        btn.titleEdgeInsets = UIEdgeInsets(top: 0, left: 2, bottom: 0, right: -2)
        return btn
    }()
    lazy var tableView: UITableView = {
        let tableView = UITableView(frame: .zero, style: .plain)
        tableView.separatorStyle = .none
        tableView.backgroundColor = .clear
        return tableView
    }()
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        unlockBtn.layer.cornerRadius = unlockBtn.frame.height*0.5
    }
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        contentView.addSubview(unlockBtn)
        contentView.addSubview(tableView)
    }
    
    override func activateConstraints() {
        super.activateConstraints()
        unlockBtn.sizeToFit()
        let width = unlockBtn.frame.width
        unlockBtn.snp.makeConstraints { (make) in
            make.trailing.equalToSuperview().offset(-20)
            make.centerY.equalTo(titleLabel)
            make.height.equalTo(38)
            make.width.equalTo(width + 28)
        }
        tableView.snp.makeConstraints { (make) in
            make.top.equalTo(titleLabel.snp.bottom).offset(20)
            make.leading.trailing.equalToSuperview()
            make.height.equalTo(280)
            make.bottom.equalToSuperview().offset(-kDeviceSafeBottomHeight)
        }
    }
    
    override func bindInteraction() {
        super.bindInteraction()
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(TRTCChorusAudienceCell.self, forCellReuseIdentifier: "TRTCChorusAudienceCell")
        unlockBtn.addTarget(self, action: #selector(unlockBtnClick), for: .touchUpInside)
        unlockBtn.isSelected = seatModel.isClosed
    }
    
    
    public let seatModel : SeatInfoModel
    private let audienceList : [AudienceInfoModel]
    
    init(frame: CGRect = .zero, viewModel: TRTCChorusViewModel, seatModel: SeatInfoModel, audienceList: [AudienceInfoModel]) {
        self.seatModel = seatModel
        self.audienceList = audienceList
        super.init(viewModel: viewModel)
        titleLabel.text = .audienceText
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    @objc func unlockBtnClick() {
        unlockBtn.isSelected = !unlockBtn.isSelected
        unlockBtn.sizeToFit()
        let width = unlockBtn.frame.width
        unlockBtn.snp.updateConstraints { (make) in
            make.width.equalTo(width+28)
        }
        unlockBtn.superview?.layoutIfNeeded()
        viewModel.clickSeatLock(isLock: unlockBtn.isSelected, model: seatModel)
        dismiss()
    }
}

extension TRTCChorusAudienceAlert : UITableViewDataSource {
    func numberOfSections(in tableView: UITableView) -> Int {
        return audienceList.count
    }
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 1
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "TRTCChorusAudienceCell", for: indexPath)
        let model = audienceList[indexPath.section]
        if let cell = cell as? TRTCChorusAudienceCell {
            cell.model = model
            cell.alertView = self
        }
        return cell
    }
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return UIView()
    }
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 20
    }
}
extension TRTCChorusAudienceAlert : UITableViewDelegate {
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
    }
}

class TRTCChorusAudienceCell: UITableViewCell {
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .clear
        selectionStyle = .none
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public var model : AudienceInfoModel? {
        didSet {
            guard let model = model  else {
                return
            }
            headImageView.kf.setImage(with: URL(string: model.userInfo.userAvatar), placeholder: nil, options: [], completionHandler: nil)
            titleLabel.text = model.userInfo.userName
            switch model.type {
            case AudienceInfoModel.TYPE_IDEL:
                agreeBtn.isHidden = false
                agreeBtn.isSelected = false
            case AudienceInfoModel.TYPE_WAIT_AGREE:
                agreeBtn.isHidden = false
                agreeBtn.isSelected = true
            case AudienceInfoModel.TYPE_IN_SEAT:
                agreeBtn.isHidden = true
            default:
                agreeBtn.isHidden = true
            }
        }
    }
    
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
        contentView.addSubview(headImageView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(agreeBtn)
    }
    
    private func activateConstraints() {
        headImageView.snp.makeConstraints { (make) in
            make.height.equalTo(64)
            make.leading.equalToSuperview().offset(20)
            make.top.bottom.equalToSuperview()
            make.width.equalTo(headImageView.snp.height)
        }
        titleLabel.snp.makeConstraints { (make) in
            make.leading.equalTo(headImageView.snp.trailing).offset(20)
            make.centerY.equalToSuperview()
            make.trailing.lessThanOrEqualTo(agreeBtn.snp.leading).offset(-20)
        }
        agreeBtn.snp.makeConstraints { (make) in
            make.trailing.equalToSuperview().offset(-20)
            make.height.equalTo(38)
            make.centerY.equalToSuperview()
            make.width.equalTo(76)
        }
    }
    
    private func bindInteraction() {
        agreeBtn.addTarget(self, action: #selector(agreeBtnClick(sender:)), for: .touchUpInside)
    }
    weak var alertView : TRTCChorusAudienceAlert?
    @objc func agreeBtnClick(sender: UIButton) {
        model?.action(sender.isSelected ? 1 : 0)
        sender.isSelected = !sender.isSelected
        alertView?.dismiss()
    }
    
    lazy var headImageView: UIImageView = {
        let imageV = UIImageView(frame: .zero)
        imageV.clipsToBounds = true
        imageV.layer.cornerRadius = 8
        return imageV
    }()
    
    lazy var titleLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Medium", size: 16)
        label.minimumScaleFactor = 0.5
        label.adjustsFontSizeToFitWidth = true
        label.textColor = UIColor(hex: "666666")
        return label
    }()
    
    lazy var agreeBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.backgroundColor = UIColor(hex: "29CC85")
        btn.layer.cornerRadius = 38*0.5
        btn.setTitle(.inviteText, for: .normal)
        btn.setTitle(.agreeText, for: .selected)
        btn.titleLabel?.textColor = .white
        btn.titleLabel?.font = UIFont(name: "PingFangSC-Medium", size: 14)
        btn.titleLabel?.adjustsFontSizeToFitWidth = true
        btn.titleLabel?.minimumScaleFactor = 0.5
        return btn
    }()
}


/// MARK: - internationalization string
fileprivate extension String {
    static let audienceText = ChorusLocalize("Demo.TRTC.Chorus.audience")
    static let unlockText = ChorusLocalize("Demo.TRTC.Chorus.unlock")
    static let lockText = ChorusLocalize("Demo.TRTC.Chorus.lock")
    static let agreeText = ChorusLocalize("Demo.TRTC.Chorus.agree")
    static let inviteText = ChorusLocalize("Demo.TRTC.Chorus.invite")
    static let earMonitorText = ChorusLocalize("Demo.TRTC.Chorus.earmonitor")
    static let backText = ChorusLocalize("Demo.TRTC.Chorus.back")
    static let bgmText = ChorusLocalize("ASKit.MainMenu.BGM")
}
