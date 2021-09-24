//
//  TRTCChorusAudienceListView.swift
//  TUIChorus
//
//  Created by adams on 2020/8/4.
//  Copyright © 2021 tencent. All rights reserved.
//

import UIKit

class TRTCChorusAudienceListView: UIView {
    private var isViewReady: Bool = false
    let viewModel: TRTCChorusViewModel
    
    init(frame: CGRect = .zero, viewModel: TRTCChorusViewModel) {
        self.viewModel = viewModel
        super.init(frame: frame)
        bindInteraction()
    }
    
    required init?(coder: NSCoder) {
        fatalError("can't init this viiew from coder")
    }
    
    let container: UIView = {
        let view = UIView.init(frame: .zero)
        view.backgroundColor = .pannelBackColor
        return view
    }()
    
    let titleContainer: UIView = {
        let view = UIView.init(frame: .zero)
        return view
    }()
    
    let titleLabel: UILabel = {
        let label = UILabel.init(frame: .zero)
        label.text = .inviteHandsupText
        label.font = UIFont.systemFont(ofSize: 16.0)
        label.textColor = UIColor.init(0xEBF4FF)
        label.textAlignment = .center
        return label
    }()
    
    let closeButton: UIButton = {
        let button = UIButton.init(type: .custom)
        button.setTitle(.closeText, for: .normal)
        return button
    }()
    
    let tableView: UITableView = {
        let tableView = UITableView.init(frame: .zero)
        tableView.register(TRTCChorusAudienceTableViewCell.self, forCellReuseIdentifier: "TRTCChorusAudienceTableViewCell")
        tableView.backgroundColor = UIColor.clear
        tableView.rowHeight = 64
        tableView.separatorStyle = .none
        return tableView
    }()
    
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else {
            return
        }
        isViewReady = true
        constructViewHierarchy() // 视图层级布局
        activateConstraints() // 生成约束（此时有可能拿不到父视图正确的frame）
    }
    
    deinit {
        
    }

    func constructViewHierarchy() {
        /// 此方法内只做add子视图操作
        addSubview(container)
        container.addSubview(titleContainer)
        titleContainer.addSubview(titleLabel)
        titleContainer.addSubview(closeButton)
        container.addSubview(tableView)
    }

    func activateConstraints() {
        /// 此方法内只给子视图做布局,使用:AutoLayout布局
        container.snp.makeConstraints { (make) in
            make.bottom.left.right.equalToSuperview()
            make.height.equalTo(418)
        }
        titleContainer.snp.makeConstraints { (make) in
            make.top.left.right.equalToSuperview()
            make.height.equalTo(56)
        }
        titleLabel.snp.makeConstraints { (make) in
            make.center.equalToSuperview()
        }
        closeButton.snp.makeConstraints { (make) in
            make.centerY.equalTo(titleLabel.snp.centerY)
            make.right.equalToSuperview().offset(-20)
        }
        tableView.snp.makeConstraints { (make) in
            make.bottom.right.left.equalToSuperview()
            make.top.equalTo(titleContainer.snp.bottom)
        }
    }

    func bindInteraction() {
        /// 此方法负责做viewModel和视图的绑定操作
        closeButton.addTarget(self, action: #selector(hide), for: .touchUpInside)
        tableView.delegate = self
        tableView.dataSource = self
    }
    
    func show() {
        isHidden = false
    }
    
    @objc
    func hide() {
        isHidden = true
    }
    
    func refreshList() {
        tableView.reloadData()
    }
}

extension TRTCChorusAudienceListView: UITableViewDelegate {
    
}

extension TRTCChorusAudienceListView: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return viewModel.memberAudienceList.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "TRTCChorusAudienceTableViewCell", for: indexPath)
        if let audienceCell = cell as? TRTCChorusAudienceTableViewCell {
            let model = viewModel.memberAudienceList[indexPath.row]
            audienceCell.setCell(model: model)
        }
        return cell
    }
}

/// MARK: - internationalization string
fileprivate extension String {
    static let closeText = ChorusLocalize("Demo.TRTC.Salon.close")
    static let inviteHandsupText = ChorusLocalize("Demo.TRTC.Chorus.invitehandsup")
}



