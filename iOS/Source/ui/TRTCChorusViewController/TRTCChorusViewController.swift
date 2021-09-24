//
//  TRTCChorusViewController.swift
//  Alamofire
//
//  Created by adams on 2021/7/14.
//

import UIKit
import TXAppBasic

protocol TRTCChorusViewModelFactory {
    func makeChorusViewModel(roomInfo: ChorusRoomInfo, roleType: ChorusRoleType) -> TRTCChorusViewModel
}

public class TRTCChorusViewController: UIViewController {
    
    let viewModel: TRTCChorusViewModel
    let toneQuality: ChorusToneQuality
    let musicDataSource: ChorusMusicService
    
    init(viewModel: TRTCChorusViewModel, toneQuality: ChorusToneQuality, musicDataSource: ChorusMusicService) {
        self.viewModel = viewModel
        self.toneQuality = toneQuality
        self.musicDataSource = musicDataSource
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: - life cycle
    public override func viewDidLoad() {
        super.viewDidLoad()
        if viewModel.isOwner {
            viewModel.createRoom(toneQuality: toneQuality.rawValue)
        } else {
            viewModel.enterRoom()
        }
    }
    
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.navigationController?.setNavigationBarHidden(true, animated: false)
    }
    
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        viewModel.refreshView()
    }
    
    public override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        self.navigationController?.setNavigationBarHidden(false, animated: false)
    }
    
    public override func loadView() {
        // Reload view in this function
        let rootView = TRTCChorusRootView.init(viewModel: viewModel)
        viewModel.rootVC = self
        viewModel.viewNavigator = self
        viewModel.musicDataSource = self.musicDataSource
        view = rootView
    }
    
    deinit {
        TRTCLog.out("deinit \(type(of: self))")
    }
}

extension TRTCChorusViewController: TRTCChorusViewNavigator {
    
    func popToPrevious() {
        self.navigationController?.popViewController(animated: true)
    }
    
    func presentAlert(viewController: UIViewController, animated: Bool, completion: (() -> Void)?) {
        self.navigationController?.present(viewController, animated: animated, completion: completion)
    }
    
}
