//
//  TRTCChorusSongSelectorTableView.swift
//  TUIChorus
//
//  Created by adams on 2021/7/15.
//

import UIKit

import Foundation

class TRTCChorusSongSelectorTableView: UIView {
    
    var dataSource: [ChorusMusicModel] {
        get {
            return viewModel.effectViewModel.musicList
        }
    }
    
    func updateDataSource() {
        tableView.reloadData()
    }
    
    lazy var tableView: UITableView = {
        let tableView = UITableView(frame: .zero, style: .plain)
        tableView.backgroundColor = .clear
        tableView.separatorStyle = .none
        tableView.showsVerticalScrollIndicator = false
        tableView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 20, right: 0)
        return tableView
    }()
    
    let viewModel: TRTCChorusViewModel
    init(viewModel: TRTCChorusViewModel, frame: CGRect = .zero) {
        self.viewModel = viewModel
        super.init(frame: frame)
        
        backgroundColor = .clear
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
        
        tableView.reloadData()
    }
    
    func constructViewHierarchy() {
        addSubview(tableView)
    }
    
    func activateConstraints() {
        tableView.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
    }
    
    func bindInteraction() {
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(TRTCChorusSongSelectorTableViewCell.self, forCellReuseIdentifier: "TRTCChorusSongSelectorTableViewCell")
    }
}

extension TRTCChorusSongSelectorTableView: UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return dataSource.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "TRTCChorusSongSelectorTableViewCell", for: indexPath)
        let model = dataSource[indexPath.row]
        if let scell = cell as? TRTCChorusSongSelectorTableViewCell {
            scell.model = model
        }
        return cell
    }
}

extension TRTCChorusSongSelectorTableView: UITableViewDelegate {
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        if let scell = cell as? TRTCChorusSongSelectorTableViewCell {
            scell.reloadSongSelectorBtnState()
        }
    }
}

class TRTCChorusSongSelectorTableViewCell: UITableViewCell {
    
    lazy var headerImageView: UIImageView = {
        let imageView = UIImageView(frame: .zero)
        imageView.contentMode = .scaleAspectFill
        return imageView
    }()
    
    lazy var titleLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Medium", size: 16)
        label.textColor = .white
        return label
    }()
    
    lazy var descLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Regular", size: 14)
        label.textColor = UIColor.init(white: 1, alpha: 0.6)
        return label
    }()
    
    lazy var songSelectBtn: UIButton = {
        let btn = UIButton(type: .custom)
        
        let norTitle = String.songSelectorText
        let norRange = NSRange(location: 0, length: norTitle.count)
        let norAttr = NSMutableAttributedString(string: norTitle)
        norAttr.addAttribute(.font, value: UIFont(name: "PingFangSC-Medium", size: 14) ?? UIFont.systemFont(ofSize: 14), range: norRange)
        norAttr.addAttribute(.foregroundColor, value: UIColor.white, range: norRange)
        btn.setAttributedTitle(norAttr, for: .normal)
        
        let selTitle = String.selectedSongText
        let selRange = NSRange(location: 0, length: selTitle.count)
        let selAttr = NSMutableAttributedString(string: selTitle)
        selAttr.addAttribute(.font, value: UIFont(name: "PingFangSC-Medium", size: 14) ?? UIFont.systemFont(ofSize: 14), range: selRange)
        selAttr.addAttribute(.foregroundColor, value: UIColor.init(white: 1, alpha: 0.4), range: selRange)
        btn.setAttributedTitle(selAttr, for: .disabled)
        
        btn.clipsToBounds = true
        btn.bounds.size = CGSize(width: 76, height: 38)
        return btn
    }()
    
    var model: ChorusMusicModel? {
        didSet {
            guard let model = model else {
                return
            }
            headerImageView.image = UIImage(named: "music_default", in: ChorusBundle(), compatibleWith: nil)
            titleLabel.text = model.musicName
            descLabel.text = model.singer
            songSelectBtn.isEnabled = !model.isSelected
        }
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .clear
        selectionStyle = .none
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        
        songSelectBtn.layer.cornerRadius = songSelectBtn.frame.height * 0.5
    }
    
    func reloadSongSelectorBtnState() {
        if songSelectBtn.isEnabled {
            let selectBtnLayer = songSelectBtn.gradient(colors: [UIColor(hex: "FF88DD")!.cgColor, UIColor(hex: "7D00BD")!.cgColor])
            selectBtnLayer.startPoint = CGPoint(x: 0, y: 0.5)
            selectBtnLayer.endPoint = CGPoint(x: 1, y: 0.5)
            
            songSelectBtn.layer.borderWidth = 0
        }
        else {
            songSelectBtn.removeGradientLayer()
            songSelectBtn.layer.borderColor = UIColor.init(white: 1, alpha: 0.4).cgColor
            songSelectBtn.layer.borderWidth = 1
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
    
    func constructViewHierarchy() {
        contentView.addSubview(headerImageView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(descLabel)
        contentView.addSubview(songSelectBtn)
    }
    
    func activateConstraints() {
        headerImageView.snp.makeConstraints { (make) in
            make.top.equalToSuperview().offset(8)
            make.bottom.equalToSuperview().offset(-8)
            make.leading.equalToSuperview().offset(20)
            make.size.equalTo(CGSize(width: 64, height: 64))
        }
        songSelectBtn.snp.makeConstraints { (make) in
            make.trailing.equalToSuperview().offset(-20)
            make.centerY.equalToSuperview()
            make.size.equalTo(CGSize(width: 76, height: 38))
        }
        titleLabel.snp.makeConstraints { (make) in
            make.bottom.equalTo(contentView.snp.centerY)
            make.leading.equalTo(headerImageView.snp.trailing).offset(16)
            make.trailing.lessThanOrEqualTo(songSelectBtn.snp.leading).offset(-10)
        }
        descLabel.snp.makeConstraints { (make) in
            make.leading.equalTo(titleLabel)
            make.top.equalTo(contentView.snp.centerY)
            make.trailing.lessThanOrEqualTo(songSelectBtn.snp.leading).offset(-10)
        }
    }
    
    func bindInteraction() {
        songSelectBtn.addTarget(self, action: #selector(songSelectBtnClick), for: .touchUpInside)
    }
    
    @objc func songSelectBtnClick() {
        guard let model = model else {
            return
        }
        if let action = model.action {
            let res = action(model)
            if res {
                songSelectBtn.isEnabled = false
                reloadSongSelectorBtnState()
            }
        }
    }
}

/// MARK: - internationalization string
fileprivate extension String {
    static let songSelectorText = ChorusLocalize("Demo.TRTC.Chorus.selectsong")
    static let selectedSongText = ChorusLocalize("Demo.TRTC.Chorus.selectedsong")
    static let permissionDeniedText = ChorusLocalize("Permission denied")
}
