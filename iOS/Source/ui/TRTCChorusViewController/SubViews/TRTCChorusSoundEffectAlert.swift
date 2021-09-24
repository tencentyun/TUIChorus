//
//  TRTCChorusSoundEffectAlert.swift
//  TUIChorus
//
//  Created by adams on 2021/8/5.
//  Copyright © 2021 Tencent. All rights reserved.
//

import Foundation
import TXAppBasic

enum AudioEffectType {
    case audition // 试听
    case musicVolume // 音乐音量
    case vocalVolume // 人声音量
    case vocalRiseFall // 人声升降调
    case voiceChange // 变声
    case reverberation // 混响
}

enum EffectType {
    case voiceChange
    case soundEffect
}

// MARK: - Sound Effect
class TRTCChorusSoundEffectAlert : TRTCChorusAlertContentView {
    
    var dataSource : [AudioEffectType] = []
    
    lazy var helpBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.setImage(UIImage(named: "helpUrl", in: ChorusBundle(), compatibleWith: nil), for: .normal)
        btn.isHidden = true
        return btn
    }()
    lazy var tableView: UITableView = {
        let tableView = UITableView(frame: .zero, style: .plain)
        tableView.showsVerticalScrollIndicator = false
        tableView.separatorStyle = .none
        tableView.backgroundColor = .clear
        return tableView
    }()
    
    let effectViewModel : TRTCChorusSoundEffectViewModel
    
    var totalHeight = 0
    
    let effectType: EffectType
    
    init(frame: CGRect = .zero, viewModel: TRTCChorusViewModel, effectType: EffectType) {
        self.effectViewModel = viewModel.effectViewModel
        self.effectType = effectType
        super.init(viewModel: viewModel)
        
        titleLabel.text = .effectTitleText
        
        if effectType == .voiceChange {
            dataSource = [.voiceChange]
            totalHeight = 80
        }
        else {
            dataSource = [.audition, .musicVolume, .vocalVolume, .vocalRiseFall, .reverberation]
            totalHeight = 120 * 1 + 52 * 4
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
    }
    
    deinit {
        debugPrint("deinit \(type(of: self))")
    }
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        contentView.addSubview(helpBtn)
        contentView.addSubview(tableView)
    }
    
    override func activateConstraints() {
        super.activateConstraints()
        helpBtn.snp.makeConstraints { (make) in
            make.leading.equalTo(titleLabel.snp.trailing).offset(4)
            make.centerY.equalTo(titleLabel)
        }
        tableView.snp.makeConstraints { (make) in
            make.top.equalTo(titleLabel.snp.bottom).offset(10)
            make.leading.trailing.bottom.equalToSuperview()
            make.height.equalTo(totalHeight + 20)
        }
    }
    
    override func bindInteraction() {
        super.bindInteraction()
        
        helpBtn.addTarget(self, action: #selector(helpBtnClick), for: .touchUpInside)
        
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(TRTCChorusSoundEffectCollectionCell.self, forCellReuseIdentifier: "TRTCChorusSoundEffectCollectionCell")
        tableView.register(TRTCChorusSoundEffectSwitchCell.self, forCellReuseIdentifier: "TRTCChorusSoundEffectSwitchCell")
        tableView.register(TRTCChorusSoundEffectDetailCell.self, forCellReuseIdentifier: "TRTCChorusSoundEffectDetailCell")
        tableView.register(TRTCChorusSoundEffectSliderCell.self, forCellReuseIdentifier: "TRTCChorusSoundEffectSliderCell")
        tableView.register(TRTCChorusSoundEffectPlayingCell.self, forCellReuseIdentifier: "TRTCChorusSoundEffectPlayingCell")
    }
    
    @objc func helpBtnClick() {
        
    }
}

extension TRTCChorusSoundEffectAlert : UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return dataSource.count
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let type = dataSource[indexPath.row]
        switch type {
        case .audition:
            let cell = tableView.dequeueReusableCell(withIdentifier: "TRTCChorusSoundEffectSwitchCell", for: indexPath)
            if let scell = cell as? TRTCChorusSoundEffectSwitchCell {
                scell.titleLabel.text = .auditionText
                scell.descLabel.text = .bringHeadphoneText
                scell.onOff.isOn = self.viewModel.voiceEarMonitor
                scell.valueChanged = { [weak self] (isOn) in
                    guard let `self` = self else { return }
                    self.viewModel.voiceEarMonitor = isOn
                }
            }
            return cell
        case .musicVolume:
            let cell = tableView.dequeueReusableCell(withIdentifier: "TRTCChorusSoundEffectSliderCell", for: indexPath)
            if let scell = cell as? TRTCChorusSoundEffectSliderCell {
                scell.titleLabel.text = .musicVolumeText
                scell.set(100, 0, Float(effectViewModel.currentMusicVolum))
                scell.valueChanged = { [weak self] (current) in
                    guard let `self` = self else { return }
                    self.effectViewModel.setVolume(music: Int(current))
                }
            }
            return cell
        case .vocalVolume:
            let cell = tableView.dequeueReusableCell(withIdentifier: "TRTCChorusSoundEffectSliderCell", for: indexPath)
            if let scell = cell as? TRTCChorusSoundEffectSliderCell {
                scell.titleLabel.text = .vocalVolumeText
                scell.set(100, 0, Float(effectViewModel.currentVocalVolume))
                scell.valueChanged = { [weak self] (current) in
                    guard let `self` = self else { return }
                    self.effectViewModel.setVolume(person: Int(current))
                }
            }
            return cell
        case .vocalRiseFall:
            let cell = tableView.dequeueReusableCell(withIdentifier: "TRTCChorusSoundEffectSliderCell", for: indexPath)
            if let scell = cell as? TRTCChorusSoundEffectSliderCell {
                scell.titleLabel.text = .vocalRiseFallText
                scell.set(1, -1, Float(effectViewModel.currentPitchVolum))
                scell.valueChanged = { [weak self] (current) in
                    guard let `self` = self else { return }
                    self.effectViewModel.setPitch(person: Double(current))
                }
            }
            return cell
        case .voiceChange:
            let cell = tableView.dequeueReusableCell(withIdentifier: "TRTCChorusSoundEffectCollectionCell", for: indexPath)
            if let scell = cell as? TRTCChorusSoundEffectCollectionCell {
                scell.dataSource = effectViewModel.voiceChangeDataSource
                scell.titleLabel.text = ""
                scell.hideTitleLabel()
            }
            return cell
        case .reverberation:
            let cell = tableView.dequeueReusableCell(withIdentifier: "TRTCChorusSoundEffectCollectionCell", for: indexPath)
            if let scell = cell as? TRTCChorusSoundEffectCollectionCell {
                scell.dataSource = effectViewModel.reverbDataSource
                scell.titleLabel.text = .reverbText
            }
            return cell
            
        }
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        let type = dataSource[indexPath.row]
        if type == .reverberation {
            return 120
        }
        else if type == .voiceChange {
            return 100
        }
        else {
            return 52
        }
    }
    
    func string2Display(second: Int) -> String {
        let min = second / 60
        let sec = second % 60
        return "\(string(fromSecond: min)):\(string(fromSecond: sec))"
    }
    
    func string(fromSecond: Int) -> String {
        if fromSecond > 9 {
            return String(fromSecond)
        }
        else {
            return "0\(fromSecond)"
        }
    }
    
}

// MARK: - Cells
class TRTCChorusSoundEffectBaseCell: UITableViewCell {
    lazy var titleLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Medium", size: 16)
        label.textColor = .white
        label.adjustsFontSizeToFitWidth = true
        return label
    }()
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        backgroundColor = .clear
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        debugPrint("deinit \(type(of: self))")
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
    func constructViewHierarchy() {
        contentView.addSubview(titleLabel)
    }
    func activateConstraints() {
        titleLabel.snp.makeConstraints { (make) in
            make.leading.equalToSuperview().offset(20)
            make.top.equalToSuperview().offset(12)
        }
    }
    func bindInteraction() {
        
    }
}

class TRTCChorusSoundEffectSwitchCell: TRTCChorusSoundEffectBaseCell {
    
    public var valueChanged: ((_ isOn: Bool)->())?
    
    lazy var descLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Regular", size: 12)
        label.textColor = UIColor.init(white: 1, alpha: 0.6)
        label.adjustsFontSizeToFitWidth = true
        label.numberOfLines = 2
        return label
    }()
    
    lazy var onOff: UISwitch = {
        let onoff = UISwitch(frame: .zero)
        onoff.onTintColor = UIColor(hex: "F95F91")
        return onoff
    }()
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        contentView.addSubview(descLabel)
        contentView.addSubview(onOff)
    }
    override func activateConstraints() {
        super.activateConstraints()
        titleLabel.snp.makeConstraints { (make) in
            make.bottom.equalToSuperview().offset(-12)
        }
        descLabel.snp.makeConstraints { (make) in
            make.leading.equalTo(titleLabel.snp.trailing).offset(8)
            make.centerY.equalTo(titleLabel)
            make.trailing.lessThanOrEqualTo(onOff.snp.leading)
        }
        onOff.snp.makeConstraints { (make) in
            make.trailing.equalToSuperview().offset(-20)
            make.centerY.equalTo(titleLabel)
        }
    }
    override func bindInteraction() {
        super.bindInteraction()
        onOff.addTarget(self, action: #selector(switchValueChanged(sender:)), for: .valueChanged)
    }
    
    @objc func switchValueChanged(sender: UISwitch) {
        if let action = valueChanged {
            action(sender.isOn)
        }
    }
}

class TRTCChorusSoundEffectSlider: UISlider {
    override func thumbRect(forBounds bounds: CGRect, trackRect rect: CGRect, value: Float) -> CGRect {
        let edge = CGFloat(4)
        var rect = rect
        rect.origin.x -= edge
        rect.size.width += 2 * edge
        return super.thumbRect(forBounds: bounds, trackRect: rect, value: value).insetBy(dx: edge, dy: edge)
    }
}

class TRTCChorusSoundEffectSliderCell: TRTCChorusSoundEffectBaseCell {
    
    public var valueChanged : ((_ value: Float)->())?
    
    public var maxValue : Float = 0 {
        didSet {
            slider.maximumValue = maxValue
        }
    }
    public var minValue : Float = 0 {
        didSet {
            slider.minimumValue = minValue
        }
    }
    public var currentValue : Float = 0 {
        didSet {
            slider.value = currentValue
        }
    }
    
    public func set(_ max: Float, _ min: Float, _ current: Float) {
        maxValue = max
        minValue = min
        currentValue = current
        updateSlider()
    }
    
    lazy var slider: TRTCChorusSoundEffectSlider = {
        let slider = TRTCChorusSoundEffectSlider(frame: .zero)
        slider.setThumbImage(UIImage(named: "Slider", in: ChorusBundle(), compatibleWith: nil), for: .normal)
        slider.minimumTrackTintColor = UIColor(hex: "F95F91")
        slider.maximumTrackTintColor = UIColor(hex: "F4F5F9")
        return slider
    }()
    
    lazy var valueLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Medium", size: 16)
        label.textColor = .white
        return label
    }()
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        contentView.addSubview(slider)
        contentView.addSubview(valueLabel)
    }
    override func activateConstraints() {
        super.activateConstraints()
        titleLabel.snp.makeConstraints { (make) in
            make.bottom.equalToSuperview().offset(-12)
            make.trailing.lessThanOrEqualTo(slider.snp.leading).offset(-8)
        }
        valueLabel.snp.makeConstraints { (make) in
            make.trailing.equalToSuperview().offset(-20)
            make.centerY.equalTo(titleLabel)
            make.width.equalTo(convertPixel(w: 50))
        }
        slider.snp.makeConstraints { (make) in
            make.leading.equalToSuperview().offset(convertPixel(w: 110))
            make.centerY.equalTo(titleLabel)
            make.trailing.equalTo(valueLabel.snp.leading).offset(-10)
        }
    }
    override func bindInteraction() {
        super.bindInteraction()
        slider.addTarget(self, action: #selector(sliderValueChanged(sender:)), for: .valueChanged)
    }
    
    @objc func sliderValueChanged(sender: UISlider) {
        updateSlider()
        if let action = valueChanged {
            action(slider.value)
        }
    }
    
    private func updateSlider() {
        if slider.maximumValue == 1 && slider.minimumValue == -1 {
            valueLabel.text = String(format: "%.2f", slider.value)
        }
        else {
            valueLabel.text = String(Int(slider.value))
        }
    }
}

class TRTCChorusSoundEffectPlayingCell: TRTCChorusSoundEffectBaseCell {
    lazy var timeLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.textColor = .black
        return label
    }()
    lazy var playBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.setImage(UIImage(named: "bgm_play", in: ChorusBundle(), compatibleWith: nil), for: .normal)
        btn.setImage(UIImage(named: "bgm_pause", in: ChorusBundle(), compatibleWith: nil), for: .selected)
        return btn
    }()
    
    public var playBtnDidClick: (()->())?
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        contentView.addSubview(playBtn)
        contentView.addSubview(timeLabel)
    }
    override func activateConstraints() {
        super.activateConstraints()
        titleLabel.snp.makeConstraints { (make) in
            make.bottom.equalToSuperview().offset(-12)
        }
        playBtn.snp.makeConstraints { (make) in
            make.trailing.equalToSuperview().offset(-20)
            make.centerY.equalTo(titleLabel)
        }
        timeLabel.snp.makeConstraints { (make) in
            make.trailing.equalTo(playBtn.snp.leading).offset(-10)
            make.centerY.equalTo(titleLabel)
        }
    }
    override func bindInteraction() {
        super.bindInteraction()
        playBtn.addTarget(self, action: #selector(playBtnClick), for: .touchUpInside)
    }
    
    @objc func playBtnClick() {
        if let action = playBtnDidClick {
            action()
        }
    }
}

class TRTCChorusSoundEffectDetailCell: TRTCChorusSoundEffectBaseCell {
    lazy var arrowImageView: UIImageView = {
        let imageV = UIImageView(image: UIImage(named: "detail", in: ChorusBundle(), compatibleWith: nil))
        return imageV
    }()
    
    lazy var descLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Regular", size: 16)
        label.textColor = UIColor(hex: "999999")
        return label
    }()
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        contentView.addSubview(descLabel)
        contentView.addSubview(arrowImageView)
    }
    override func activateConstraints() {
        super.activateConstraints()
        titleLabel.snp.makeConstraints { (make) in
            make.bottom.equalToSuperview().offset(-12)
        }
        arrowImageView.snp.makeConstraints { (make) in
            make.trailing.equalToSuperview().offset(-20)
            make.centerY.equalTo(titleLabel)
        }
        descLabel.snp.makeConstraints { (make) in
            make.leading.greaterThanOrEqualTo(titleLabel.snp.trailing).offset(8)
            make.centerY.equalTo(titleLabel)
            make.trailing.equalTo(arrowImageView.snp.leading).offset(-10)
        }
    }
    override func bindInteraction() {
        super.bindInteraction()
        
    }
}

class TRTCChorusSoundEffectCollectionCell: TRTCChorusSoundEffectBaseCell {
    
    var currentSelect: Int = 0
    
    var dataSource : [TRTCAudioEffectCellModel] = [] {
        didSet {
            for (i, model) in dataSource.enumerated() {
                if model.selected {
                    currentSelect = i
                    break
                }
            }
            collectionView.reloadData()
            collectionView.layoutIfNeeded()
            collectionView.selectItem(at: IndexPath(item: currentSelect, section: 0), animated: true, scrollPosition: .left)
        }
    }
    
    private var isHideTitleLabel = false
    private var isViewReady = false
    func hideTitleLabel() {
        isHideTitleLabel = true
        if isViewReady {
            collectionView.snp.remakeConstraints { (make) in
                make.top.equalToSuperview().offset(12)
                make.leading.trailing.bottom.equalToSuperview()
                make.height.equalTo(75)
            }
        }
    }
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        contentView.addSubview(collectionView)
    }
    override func activateConstraints() {
        super.activateConstraints()
        if isHideTitleLabel {
            collectionView.snp.makeConstraints { (make) in
                make.top.equalToSuperview().offset(12)
                make.leading.trailing.bottom.equalToSuperview()
                make.height.equalTo(75)
            }
        }
        else {
            collectionView.snp.makeConstraints { (make) in
                make.top.equalTo(titleLabel.snp.bottom).offset(10)
                make.leading.trailing.bottom.equalToSuperview()
                make.height.equalTo(75)
            }
        }
    }
    override func bindInteraction() {
        super.bindInteraction()
        collectionView.delegate = self
        collectionView.dataSource = self
        collectionView.register(TRTCChorusSoundEffectCellForCollectionCell.self, forCellWithReuseIdentifier: "TRTCChorusSoundEffectCellForCollectionCell")
        isViewReady = true
    }
    
    lazy var collectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.itemSize = CGSize(width: 50, height: 75)
        layout.minimumLineSpacing = 10
        layout.minimumInteritemSpacing = 10
        layout.scrollDirection = .horizontal
        let collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        collectionView.showsHorizontalScrollIndicator = false
        collectionView.backgroundColor = .clear
        collectionView.contentInset = UIEdgeInsets(top: 0, left: 20, bottom: 0, right: 20)
        return collectionView
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .clear
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
extension TRTCChorusSoundEffectCollectionCell : UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return dataSource.count
    }
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "TRTCChorusSoundEffectCellForCollectionCell", for: indexPath)
        if let scell = cell as? TRTCChorusSoundEffectCellForCollectionCell {
            let model = dataSource[indexPath.item]
            scell.model = model
        }
        return cell
    }
}
extension TRTCChorusSoundEffectCollectionCell : UICollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let model = dataSource[indexPath.item]
        if let action = model.action {
            action()
        }
    }
}
class TRTCChorusSoundEffectCellForCollectionCell: UICollectionViewCell {
    
    var model : TRTCAudioEffectCellModel? {
        didSet {
            guard let model = model else {
                return
            }
            headImageView.image = model.icon
            headImageView.highlightedImage = model.selectIcon
            titleLabel.text = model.title
            isSelected = model.selected
        }
    }
    
    override var isSelected: Bool {
        didSet {
            guard let model = model else {
                return
            }
            model.selected = isSelected
            headImageView.isHighlighted = isSelected
            titleLabel.isHighlighted = isSelected
        }
    }
    
    lazy var headImageView: UIImageView = {
        let imageV = UIImageView(frame: .zero)
        imageV.contentMode = .scaleAspectFill
        imageV.clipsToBounds = true
        return imageV
    }()
    
    lazy var titleLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Regular", size: 12)
        label.textColor = UIColor(hex: "666666")
        label.highlightedTextColor = UIColor(hex: "006EFF")
        label.adjustsFontSizeToFitWidth = true
        label.textAlignment = .center
        return label
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        backgroundColor = .clear
        
        contentView.addSubview(headImageView)
        contentView.addSubview(titleLabel)
        headImageView.snp.makeConstraints { (make) in
            make.top.leading.trailing.equalToSuperview()
            make.height.equalTo(headImageView.snp.width)
        }
        titleLabel.snp.makeConstraints { (make) in
            make.top.equalTo(headImageView.snp.bottom).offset(4)
            make.leading.trailing.centerX.equalToSuperview()
        }
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        headImageView.layer.cornerRadius = headImageView.frame.height * 0.5
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

/// MARK: - internationalization string
fileprivate extension String {
    static let effectTitleText = ChorusLocalize("ASKit.MainMenu.Title")
    static let voiceChangeText = ChorusLocalize("ASKit.MainMenu.VoiceChangeTitle")
    static let reverbText = ChorusLocalize("ASKit.MainMenu.Reverberation")
    static let auditionText = ChorusLocalize("ASKit.MusicSelectMenu.Title")
    static let bringHeadphoneText = ChorusLocalize("Demo.TRTC.Chorus.useearphones")
    static let copyrightText = ChorusLocalize("Demo.TRTC.Chorus.copyrights")
    static let selectMusicText = ChorusLocalize("ASKit.MainMenu.SelectMusic")
    static let musicVolumeText = ChorusLocalize("ASKit.MainMenu.MusicVolum")
    static let vocalVolumeText = ChorusLocalize("ASKit.MainMenu.PersonVolum")
    static let vocalRiseFallText = ChorusLocalize("ASKit.MainMenu.PersonPitch")
}
