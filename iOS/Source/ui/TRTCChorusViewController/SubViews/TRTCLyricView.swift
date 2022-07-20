//
//  TRTCLyricView.swift
//  TUIChorus
//
//  Created by adams on 2021/8/5.
//  Copyright © 2022 Tencent. All rights reserved.

import Foundation

class TRTCLyricView: UIView {
    
    public var currentMusicID: Int32 = 0
    private var reciprocalThreeSecond = 3
    
    lazy var containerView: UIView = {
        let view = UIView.init()
        view.backgroundColor = .clear
        return view
    }()
    
    lazy var bgView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "lyric_bg", in: ChorusBundle(), compatibleWith: nil))
        return imageView
    }()
    
    lazy var seatIndexLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Regular", size: 12)
        label.textColor = .white
        return label
    }()
    
    lazy var userNameLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Regular", size: 12)
        label.textColor = .white
        return label
    }()
    
    lazy var musicIcon: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "musicIcon", in: ChorusBundle(), compatibleWith: nil))
        return imageView
    }()
    
    lazy var musicNameLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Regular", size: 12)
        label.textColor = .white
        return label
    }()
    
    lazy var placeholderLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Regular", size: 14)
        label.textColor = .white
        label.textAlignment = .center
        label.numberOfLines = 2
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.5
        label.isHidden = true
        label.text = .placeholderText
        return label
    }()
    
    lazy var reciprocalLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Regular", size: 60)
        label.textColor = .white
        label.textAlignment = .center
        label.numberOfLines = 1
        label.adjustsFontSizeToFitWidth = true
        label.isHidden = true
        return label
    }()
    
    lazy var voiceChangeBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.setImage(UIImage(named: "room_bgmusic", in: ChorusBundle(), compatibleWith: nil), for: .normal)
        btn.adjustsImageWhenHighlighted = false
        btn.isHidden = true
        return btn
    }()
    
    lazy var soundEffectBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.setImage(UIImage(named: "tuning", in: ChorusBundle(), compatibleWith: nil), for: .normal)
        btn.adjustsImageWhenHighlighted = false
        btn.isHidden = true
        return btn
    }()
    
    lazy var songSelectorBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.setTitle(.songSelectorText, for: .normal)
        btn.clipsToBounds = true
        btn.titleLabel?.font = UIFont(name: "PingFangSC-Medium", size: 14)
        btn.titleLabel?.textColor = .white
        return btn
    }()
    
    lazy var startChorusBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.setTitle(.startChorusText, for: .normal)
        btn.clipsToBounds = true
        btn.isHidden = true
        btn.titleLabel?.font = UIFont(name: "PingFangSC-Medium", size: 14)
        btn.titleLabel?.numberOfLines = 2
        btn.titleEdgeInsets = .init(top: 0, left: 2, bottom: 0, right: 2)
        btn.titleLabel?.textAlignment = .center
        btn.titleLabel?.textColor = .white
        return btn
    }()
    
    lazy var reciprocalTimer: Timer = {
        let timer = Timer.scheduledTimer(timeInterval: 1, target: self, selector: #selector(reciprocalThreeSecondToPlay), userInfo: nil, repeats: true)
        return timer
    }()
    
    public lazy var lrcView: TUIVTTView = {
        let view = TUIVTTView()
        return view
    }()
    
    let viewModel: TRTCChorusViewModel
    
    init(frame: CGRect = .zero, viewModel: TRTCChorusViewModel) {
        self.viewModel = viewModel
        super.init(frame: frame)
        clipsToBounds = true
        layer.cornerRadius = 12
        
        viewModel.effectViewModel.viewResponder = self
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        debugPrint("deinit \(type(of: self))")
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        let bgGradientLayer = gradient(colors: [UIColor(hex: "5A1EA8")!.cgColor, UIColor(hex: "491192")!.cgColor])
        bgGradientLayer.startPoint = CGPoint(x: 0.5, y: 0)
        bgGradientLayer.endPoint = CGPoint(x: 0.5, y: 1)
        
        let selectBtnLayer = songSelectorBtn.gradient(colors: [UIColor(hex: "FF88DD")!.cgColor, UIColor(hex: "7D00BD")!.cgColor])
        selectBtnLayer.startPoint = CGPoint(x: 0, y: 0.5)
        selectBtnLayer.endPoint = CGPoint(x: 1, y: 0.5)
        songSelectorBtn.layer.cornerRadius = songSelectorBtn.frame.height * 0.5
        
        let startChorusBtnLayer = startChorusBtn.gradient(colors: [UIColor(hex: "FF88DD")!.cgColor, UIColor(hex: "7D00BD")!.cgColor])
        startChorusBtnLayer.startPoint = CGPoint(x: 0, y: 0.5)
        startChorusBtnLayer.endPoint = CGPoint(x: 1, y: 0.5)
        startChorusBtn.layer.cornerRadius = startChorusBtn.frame.height * 0.5
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
        updateLrcView(music: viewModel.effectViewModel.currentPlayingModel)
        setLrcURL(lrcString: viewModel.effectViewModel.currentPlayingModel?.lrcUrl)
    }
    
    func setLrcURL(lrcString: String?) {
        if let lrcString = lrcString {
            lrcView.lrcFileUrl = URL(fileURLWithPath: lrcString)
        } else {
            lrcView.lrcFileUrl = nil
        }
    }
    
    func updateLrcView(music: ChorusMusicModel?) {
        if let music = music {
            setMusicDetail(show: true)
            seatIndexLabel.text = LocalizeReplaceXX(.seatIndexText, "\(music.seatIndex + 1)")
            userNameLabel.text = music.bookUserName
            musicNameLabel.text = music.musicName
            currentMusicID = music.musicID
        } else {
            currentMusicID = 0
            setMusicDetail(show: false)
        }
    }
    
    func setMusicDetail(show: Bool) {
        if placeholderLabel.isHidden != show {
            if show {
                songSelectorBtn.snp.remakeConstraints { (make) in
                    make.trailing.equalToSuperview().offset(-8)
                    make.top.equalToSuperview().offset(8)
                    make.size.equalTo(CGSize(width: 76, height: 38))
                }
                containerView.snp.remakeConstraints { make in
                    make.trailing.equalToSuperview().offset(-8)
                    make.top.equalToSuperview().offset(8)
                    make.size.equalTo(CGSize(width: 76, height: 38))
                }
            }
            else {
                songSelectorBtn.snp.remakeConstraints { (make) in
                    make.centerX.equalToSuperview()
                    make.top.equalTo(self.snp.centerY).offset(10)
                    make.size.equalTo(CGSize(width: 76, height: 38))
                }
                containerView.snp.remakeConstraints { make in
                    make.trailing.equalToSuperview().offset(-8)
                    make.top.equalToSuperview().offset(8)
                    make.size.equalTo(CGSize(width: 0, height: 38))
                }
            }
            songSelectorBtn.setNeedsLayout()
        }
        lrcView.isHidden = !show
        seatIndexLabel.isHidden = !show
        userNameLabel.isHidden = !show
        musicNameLabel.isHidden = !show
        musicIcon.isHidden = !show
        placeholderLabel.isHidden = show
    }
    
    @objc func voiceChangeBtnClick() {
        let alert = TRTCChorusSoundEffectAlert(viewModel: viewModel, effectType: .voiceChange)
        alert.titleLabel.text = .voiceChangeTitleText
        superview?.addSubview(alert)
        alert.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        alert.layoutIfNeeded()
        alert.show()
    }
    
    @objc func soundEffectBtnClick() {
        let alert = TRTCChorusSoundEffectAlert(viewModel: viewModel, effectType: .soundEffect)
        superview?.addSubview(alert)
        alert.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        alert.layoutIfNeeded()
        alert.show()
    }
    
    lazy var songSelectorAlert: TRTCChorusSongSelectorAlert = {
        let alert = TRTCChorusSongSelectorAlert(viewModel: viewModel)
        return alert
    }()
    
    @objc
    func songSelectorBtnClick() {
        if songSelectorBtn.titleLabel?.text == .startChorusText {
            reciprocalTimer.fireDate = .distantPast
        } else {
            if songSelectorAlert.superview == nil {
                superview?.addSubview(songSelectorAlert)
                songSelectorAlert.snp.makeConstraints { (make) in
                    make.edges.equalToSuperview()
                }
                songSelectorAlert.layoutIfNeeded()
            }
            songSelectorAlert.show()
        }
    }
    
    @objc
    func startChorusBtnClick() {
        if viewModel.isOwner {
            if let musicModel = viewModel.currentMusicModel {
                startChorusBtn.isHidden = true
                reciprocalLabel.isHidden = false
                viewModel.effectViewModel.playMusic(musicModel)
                reciprocalTimer.fireDate = .distantPast
            }
        } else {
            viewModel.effectViewModel.bgmID = currentMusicID
            startChorusBtn.isHidden = true
            reciprocalLabel.isHidden = false
            reciprocalTimer.fireDate = .distantPast
        }
    }
    
    @objc
    func reciprocalThreeSecondToPlay() {
        if reciprocalThreeSecond < 1 {
            resetReciprocalStatus()
        } else {
            reciprocalLabel.isHidden = false
            reciprocalLabel.text = "\(reciprocalThreeSecond)"
            reciprocalThreeSecond -= 1
        }
    }
    
    private func resetReciprocalStatus() {
        reciprocalTimer.fireDate = .distantFuture
        reciprocalThreeSecond = 3
        reciprocalLabel.isHidden = true
        startChorusBtn.isHidden = true
        placeholderLabel.text = .placeholderText
    }
    
    public func cleanTimer() {
        reciprocalTimer.fireDate = .distantFuture
        reciprocalTimer.invalidate()
    }
    
    public func checkBtnShouldHidden() {
        if viewModel.userType == .audience {
            voiceChangeBtn.isHidden = true
            soundEffectBtn.isHidden = true
        } else {
            voiceChangeBtn.isHidden = false
            soundEffectBtn.isHidden = false
        }
    }
    
}

extension TRTCLyricView: TRTCChorusSoundEffectViewResponder {
    func showStartAnimationAndPlay(startDelay: Int) {
        if reciprocalLabel.isHidden == true {
            reciprocalThreeSecond = startDelay >= 0 ? startDelay : 0
            reciprocalThreeSecond = (reciprocalThreeSecond + 500) / 1000
            startChorusBtnClick()
        }
    }
    
    func onSelectedMusicListChanged() {
        songSelectorAlert.reloadSelectedSongView(dataSource: viewModel.effectViewModel.musicSelectedList)
    }
    
    func onMusicListChanged() {
        songSelectorAlert.reloadSongSelectorView(dataSource: viewModel.effectViewModel.musicList)
        if viewModel.isOwner && viewModel.effectViewModel.currentPlayingModel == nil {
            startChorusBtn.isHidden = false
        }
        
        if let musicModel = viewModel.effectViewModel.musicSelectedList.first {
            updateLrcView(music: musicModel)
        } else {
            updateLrcView(music: nil)
        }
    }
    
    func bgmOnPrepareToPlay(musicID: Int32) {
        guard musicID != 0 else {
            setLrcURL(lrcString: nil)
            return
        }
        var model: ChorusMusicModel?
        if let current = viewModel.effectViewModel.currentPlayingModel {
            if current.musicID == musicID {
                model = current
            }
        }
        if model == nil {
            for selected in viewModel.effectViewModel.musicSelectedList {
                if selected.music.musicID == musicID {
                    model = selected
                    break
                }
            }
        }
        if model == nil {
            for music in viewModel.effectViewModel.musicList {
                if music.music.musicID == musicID {
                    model = music
                    break
                }
            }
        }
        if model != nil {
            setLrcURL(lrcString: model?.lrcUrl)
        }
    }
    
    func bgmOnPlaying(musicID: Int32, current: Double, total: Double) {
        if musicID == currentMusicID {
            lrcView.currentTime = current
        }
    }
    
    func bgmOnCompletePlaying() {
        
    }
    
    func onManageSongBtnClick() {
        if songSelectorAlert.superview == nil {
            superview?.addSubview(songSelectorAlert)
            songSelectorAlert.snp.makeConstraints { (make) in
                make.edges.equalToSuperview()
            }
            songSelectorAlert.layoutIfNeeded()
        }
        songSelectorAlert.show(index: 1)
    }
}

// Layout
extension TRTCLyricView {
    func constructViewHierarchy() {
        
        addSubview(bgView)
        
        addSubview(seatIndexLabel)
        addSubview(userNameLabel)
        addSubview(musicIcon)
        addSubview(musicNameLabel)
        
        addSubview(placeholderLabel)
        
        addSubview(voiceChangeBtn)
        addSubview(soundEffectBtn)
        addSubview(containerView)
        addSubview(startChorusBtn)
        addSubview(songSelectorBtn)
        addSubview(reciprocalLabel)
        
        addSubview(lrcView)
    }
    
    func activateConstraints() {
        
        bgView.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        
        seatIndexLabel.snp.makeConstraints { (make) in
            make.leading.equalToSuperview().offset(12)
            make.top.equalToSuperview().offset(12)
        }
        userNameLabel.snp.makeConstraints { (make) in
            make.leading.equalTo(seatIndexLabel.snp.trailing).offset(8)
            make.centerY.equalTo(seatIndexLabel)
            make.right.equalTo(voiceChangeBtn.snp.left).offset(-4)
        }
        musicIcon.snp.makeConstraints { (make) in
            make.leading.equalTo(seatIndexLabel)
            make.centerY.equalTo(seatIndexLabel.snp.bottom).offset(13)
            make.size.equalTo(CGSize(width: 16, height: 16))
        }
        musicNameLabel.snp.makeConstraints { (make) in
            make.leading.equalTo(musicIcon.snp.trailing).offset(4)
            make.centerY.equalTo(musicIcon)
        }
        containerView.snp.makeConstraints { make in
            make.trailing.equalToSuperview().offset(-8)
            make.top.equalToSuperview().offset(8)
            make.size.equalTo(CGSize(width: 0, height: 38))
        }
        startChorusBtn.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(self.snp.centerY).offset(10)
            make.size.equalTo(CGSize(width: 76, height: 38))
        }
        songSelectorBtn.snp.makeConstraints { (make) in
            make.trailing.equalToSuperview().offset(-8)
            make.top.equalToSuperview().offset(8)
            make.size.equalTo(CGSize(width: 76, height: 38))
        }
        soundEffectBtn.snp.makeConstraints { (make) in
            make.trailing.equalTo(containerView.snp.leading).offset(-10)
            make.centerY.equalTo(containerView)
            make.size.equalTo(CGSize(width: 32, height: 32))
        }
        voiceChangeBtn.snp.makeConstraints { (make) in
            make.trailing.equalTo(soundEffectBtn.snp.leading).offset(-10)
            make.centerY.size.equalTo(soundEffectBtn)
        }
        placeholderLabel.snp.makeConstraints { (make) in
            make.bottom.equalTo(self.snp.centerY)
            make.centerX.equalToSuperview()
            make.leading.equalToSuperview().offset(20)
            make.trailing.equalToSuperview().offset(-20)
        }
        reciprocalLabel.snp.makeConstraints { make in
            make.centerX.equalTo(startChorusBtn.snp.centerX)
            make.centerY.equalTo(startChorusBtn.snp.centerY)
            make.size.equalTo(CGSize.init(width: 44, height: 44))
        }
        lrcView.snp.makeConstraints { (make) in
            make.bottom.equalToSuperview().offset(-34)
            make.leading.equalToSuperview().offset(12)
            make.trailing.equalToSuperview().offset(-12)
        }
    }
    
    func bindInteraction() {
        voiceChangeBtn.addTarget(self, action: #selector(voiceChangeBtnClick), for: .touchUpInside)
        soundEffectBtn.addTarget(self, action: #selector(soundEffectBtnClick), for: .touchUpInside)
        songSelectorBtn.addTarget(self, action: #selector(songSelectorBtnClick), for: .touchUpInside)
        startChorusBtn.addTarget(self, action: #selector(startChorusBtnClick), for: .touchUpInside)
    }
}

/// MARK: - internationalization string
fileprivate extension String {
    static let songSelectorText = ChorusLocalize("Demo.TRTC.Chorus.selectsong")
    static let voiceChangeTitleText = ChorusLocalize("ASKit.MainMenu.VoiceChangeTitle")
    static let placeholderText = ChorusLocalize("Demo.TRTC.Chorus.nosongs")
    static let seatIndexText = ChorusLocalize("Demo.TRTC.Chorus.xxmic")
    static let notInSeatText = ChorusLocalize("Demo.TRTC.Chorus.onlyanchorcanoperation")
    static let startChorusText = ChorusLocalize("Demo.TRTC.Chorus.StartChorus")
}
