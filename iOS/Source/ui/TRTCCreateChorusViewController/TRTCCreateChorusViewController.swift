//
//  TRTCCreateChorusViewController.swift
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

import UIKit
import TXAppBasic

protocol TRTCCreateChorusViewModelFactory {
    func makeCreateChorusViewModel() -> TRTCCreateChorusViewModel
}

public class TRTCCreateChorusViewController: UIViewController {

    public var screenShot : UIView?
    let viewModel: TRTCCreateChorusViewModel
    let musicDataSource: ChorusMusicService
    
    init(viewModel: TRTCCreateChorusViewModel, musicDataSource: ChorusMusicService) {
        self.viewModel = viewModel
        self.musicDataSource = musicDataSource
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        TRTCLog.out("deinit \(type(of: self))")
    }
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        title = .controllerTitle
        
        let backBtn = UIButton(type: .custom)
        backBtn.setImage(UIImage(named: "navigationbar_back", in: ChorusBundle(), compatibleWith: nil), for: .normal)
        backBtn.addTarget(self, action: #selector(cancel), for: .touchUpInside)
        backBtn.sizeToFit()
        let backItem = UIBarButtonItem(customView: backBtn)
        self.navigationItem.leftBarButtonItem = backItem
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: true)
    }
    
    public override func loadView() {
        let rootView = TRTCCreateChorusRootView.init(viewModel: viewModel, screenShot: screenShot)
        viewModel.viewResponder = self
        viewModel.musicDataSource = self.musicDataSource
        view = rootView
    }
    
    /// 取消
    @objc func cancel() {
        navigationController?.popViewController(animated: true)
    }
}

extension TRTCCreateChorusViewController: TRTCCreateChorusViewResponder {
    public func push(viewController: UIViewController) {
        navigationController?.pushViewController(viewController, animated: true)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            guard let `self` = self else { return }
            guard let vcs = self.navigationController?.viewControllers else { return }
            var controllers = vcs
            if let index = controllers.firstIndex(of: self) {
                controllers.remove(at: index)
                self.navigationController?.viewControllers = controllers
            }
        }
    }
    
    public func pop() {
        navigationController?.popViewController(animated: false)
    }
    
}

private extension String {
    static let controllerTitle = ChorusLocalize("Demo.TRTC.Chorus.createvoicechatroom")
}
