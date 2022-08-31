//
//  TRTCChorusSongSelectorAlert.swift
//  TUIChorus
//
//  Created by adams on 2021/7/15.
//  Copyright © 2022 Tencent. All rights reserved.

import UIKit
import TXAppBasic

enum SongSelectorAlertType {
    case songSelector // 点歌
    case selectedSongs // 已点
}

class TRTCChorusSongSelectorAlert: TRTCChorusAlertContentView {

    lazy var containerView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = .clear
        return view
    }()
    
    lazy var titleContainerView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = .clear
        return view
    }()
    
    lazy var scrollView: UIScrollView = {
        let scrollView = UIScrollView(frame: .zero)
        scrollView.backgroundColor = .clear
        scrollView.isPagingEnabled = true
        scrollView.contentInsetAdjustmentBehavior = .never
        scrollView.showsVerticalScrollIndicator = false
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.delegate = self
        return scrollView
    }()
    
    lazy var songSelectorBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.adjustsImageWhenHighlighted = false
        btn.setAttrTitle(.songSelectorText)
        return btn
    }()
    
    lazy var selectedSongBtn: UIButton = {
        let btn = UIButton(type: .custom)
        btn.adjustsImageWhenHighlighted = false
        btn.setAttrTitle(.selectedSongText)
        return btn
    }()
    
    lazy var btnSelectLineView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = UIColor(hex: "F95F91")
        view.layer.cornerRadius = 2
        return view
    }()
    
    lazy var marginLineView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = UIColor.init(white: 1, alpha: 0.1)
        return view
    }()
    
    lazy var songSelectorView: TRTCChorusSongSelectorTableView = {
        let view = TRTCChorusSongSelectorTableView(viewModel: viewModel)
        return view
    }()
    
    lazy var selectedView: TRTCChorusSelectedSongTableView = {
        let view = TRTCChorusSelectedSongTableView(viewModel: viewModel)
        return view
    }()
    
    func reloadSongSelectorView(dataSource: [ChorusMusicModel]) {
        songSelectorView.updateDataSource()
    }

    func reloadSelectedSongView(dataSource: [ChorusMusicModel]) {
        selectedView.updateDataSource()
    }
    
    private func configScrollView() {
        
        let containerView = UIView(frame: .zero)
        scrollView.addSubview(containerView)
        containerView.snp.makeConstraints { (make) in
            make.edges.height.equalToSuperview()
        }
        
        containerView.addSubview(songSelectorView)
        songSelectorView.snp.makeConstraints { (make) in
            make.leading.top.bottom.equalToSuperview()
            make.width.equalTo(scrollView)
        }
        songSelectorView.updateDataSource()
        
        containerView.addSubview(selectedView)
        selectedView.snp.makeConstraints { (make) in
            make.trailing.top.bottom.equalToSuperview()
            make.width.equalTo(scrollView)
            make.leading.equalTo(songSelectorView.snp.trailing)
        }
        selectedView.updateDataSource()
    }
    
    func refreshScrollEnable() {
        scrollView.isScrollEnabled = !viewModel.isOwner && viewModel.userType == .audience
    }
    
    override init(frame: CGRect = .zero, viewModel: TRTCChorusViewModel) {
        super.init(viewModel: viewModel)
        titleLabel.text = ""
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func show(index: Int) {
        super.show()
        
        selectBtn(index == 0 ? songSelectorBtn : selectedSongBtn)
        scrollView.setContentOffset(CGPoint(x: scrollView.frame.width * CGFloat(index), y: 0), animated: false)
        refreshScrollEnable()
    }
    
    override func show() {
        super.show()
        selectBtn(songSelectorBtn)
        scrollView.setContentOffset(.zero, animated: false)
        refreshScrollEnable()
    }
    
    override func constructViewHierarchy() {
        super.constructViewHierarchy()
        contentView.addSubview(containerView)
        containerView.addSubview(titleContainerView)
        containerView.addSubview(scrollView)
        
        titleContainerView.addSubview(songSelectorBtn)
        titleContainerView.addSubview(selectedSongBtn)
        titleContainerView.addSubview(btnSelectLineView)
        titleContainerView.addSubview(marginLineView)
    }
    
    override func activateConstraints() {
        super.activateConstraints()
        
        containerView.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        
        titleContainerView.snp.makeConstraints { (make) in
            make.top.equalToSuperview().offset(40)
            make.leading.trailing.equalToSuperview()
        }
        
        songSelectorBtn.snp.makeConstraints { (make) in
            make.trailing.equalTo(titleContainerView.snp.centerX).offset(-30)
            make.top.equalToSuperview()
        }
        
        selectedSongBtn.snp.makeConstraints { (make) in
            make.leading.equalTo(titleContainerView.snp.centerX).offset(30)
            make.top.equalTo(songSelectorBtn)
        }
        
        btnSelectLineView.snp.makeConstraints { (make) in
            make.top.equalTo(songSelectorBtn.snp.bottom).offset(4)
            make.size.equalTo(CGSize(width: 16, height: 4))
            make.centerX.equalTo(songSelectorBtn)
        }
        
        marginLineView.snp.makeConstraints { (make) in
            make.top.equalTo(btnSelectLineView.snp.bottom)
            make.leading.trailing.equalToSuperview()
            make.height.equalTo(1)
            make.bottom.equalToSuperview()
        }
        
        scrollView.snp.makeConstraints { (make) in
            make.top.equalTo(titleContainerView.snp.bottom)
            make.leading.trailing.bottom.equalToSuperview()
            make.height.equalTo(ScreenHeight*2/3)
        }
    }
    
    override func bindInteraction() {
        super.bindInteraction()
        
        songSelectorBtn.addTarget(self, action: #selector(songSelectorBtnClick), for: .touchUpInside)
        selectedSongBtn.addTarget(self, action: #selector(selectedSongBtnClick), for: .touchUpInside)
        
        viewModel.effectViewModel.reloadMusicList()
        viewModel.effectViewModel.reloadSelectedMusicList()
        
        configScrollView()
    }
}

fileprivate extension TRTCChorusSongSelectorAlert {
    @objc func songSelectorBtnClick() {
        selectBtn(songSelectorBtn)
        scrollView.setContentOffset(.zero, animated: true)
    }
    
    @objc func selectedSongBtnClick() {
        selectBtn(selectedSongBtn)
        scrollView.setContentOffset(CGPoint(x: scrollView.frame.width, y: 0), animated: true)
    }
    
    func selectBtn(_ btn: UIButton) {
        switch btn {
        case songSelectorBtn:
            songSelectorBtn.isSelected = true
            selectedSongBtn.isSelected = false
        case selectedSongBtn:
            songSelectorBtn.isSelected = false
            selectedSongBtn.isSelected = true
        default:
            break
        }
    }
}

extension TRTCChorusSongSelectorAlert: UIScrollViewDelegate {
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let width = scrollView.frame.width
        let offsetx = scrollView.contentOffset.x
        let progress = offsetx / width
        
        if progress < 0 || progress > 1 {
            return
        }
        let lineMax = selectedSongBtn.center.x - songSelectorBtn.center.x
        btnSelectLineView.transform = CGAffineTransform(translationX: lineMax * progress, y: 0)
    }
    func scrollViewWillEndDragging(_ scrollView: UIScrollView, withVelocity velocity: CGPoint, targetContentOffset: UnsafeMutablePointer<CGPoint>) {
        
        if targetContentOffset.pointee.x == 0 {
            selectBtn(songSelectorBtn)
        }
        else {
            selectBtn(selectedSongBtn)
        }
    }
}

fileprivate extension UIButton {
    func setAttrTitle(_ title: String) {
        
        let totalRange = NSRange(location: 0, length: title.count)
        
        let selAttr = NSMutableAttributedString(string: title)
        selAttr.addAttribute(.font, value: UIFont(name: "PingFangSC-Medium", size: 18) ?? UIFont.systemFont(ofSize: 18), range: totalRange)
        selAttr.addAttribute(.foregroundColor, value: UIColor(hex: "F95F91") ?? .systemPink, range: totalRange)
        setAttributedTitle(selAttr, for: .selected)
        
        let norAttr = NSMutableAttributedString(string: title)
        norAttr.addAttribute(.font, value: UIFont(name: "PingFangSC-Regular", size: 18) ?? UIFont.systemFont(ofSize: 18), range: totalRange)
        norAttr.addAttribute(.foregroundColor, value: UIColor.init(white: 1, alpha: 0.6), range: totalRange)
        setAttributedTitle(norAttr, for: .normal)
    }
}

/// MARK: - internationalization string
fileprivate extension String {
    static let songSelectorText = chorusLocalize("Demo.TRTC.Chorus.Songorder")
    static let selectedSongText = chorusLocalize("Demo.TRTC.Chorus.ordered")
}

