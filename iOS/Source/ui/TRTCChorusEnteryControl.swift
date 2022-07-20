//
//  TRTCChorusEnteryControl.swift
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//  Copyright © 2022 Tencent. All rights reserved.

import UIKit
import TXAppBasic

public protocol TRTCChorusEnteryControlDelegate: NSObject {
    func chorusCreateRoom(roomId: String, success: @escaping () -> Void, failed: @escaping (Int32, String) -> Void)
    func chorusDestroyRoom(roomId: String, success: @escaping () -> Void, failed: @escaping (Int32, String) -> Void)
    func generateRTMPURL(handler: @escaping ((String, String)?) -> Void)
}

/// ViewModel可视为MVC架构中的Controller层
/// 负责语音聊天室控制器和ViewModel依赖注入，以及公用参数的传递
/// ViewModel、ViewController
/// 注意：该类负责生成所有UI层的ViewController、ViewModel。慎重持有ui层的成员变量，否则很容易发生循环引用。持有成员变量时要慎重！！！！
public class TRTCChorusEnteryControl: NSObject {
    public weak var delegate: TRTCChorusEnteryControlDelegate?
    
    deinit {
        TRTCLog.out("deinit \(type(of: self))")
    }
    
    /*
     TRTCChorus为可销毁单例。
     在Demo中，可以通过shardInstance（OC）shared（swift）获取或生成单例对象
     销毁单例对象后，需要再次调用sharedInstance接口重新生成实例。
     该方法在ChorusListRoomViewModel、CreateChorusViewModel、ChorusViewModel中调用。
     由于是可销毁单例，将对象生成防止在这里的目的为统一管理单例生成路径，方便维护
     */
    private var chorusRoom: TRTCChorusRoom?
    
    /// 获取ChorusRoom
    /// - Returns: 返回ChorusRoom单例
    public func getChorusRoom() -> TRTCChorusRoom {
        if let room = chorusRoom {
            return room
        }
        chorusRoom = TRTCChorusRoom.shared()
        return chorusRoom!
    }
    
    /*
     在无需使用ChorusRoom的场景，可以将单例对象销毁。
     例如：退出登录时。
     在本Demo中没有调用到改销毁方法。
    */
    /// 销毁ChorusRoom单例
    func clearChorusRoom() {
        TRTCChorusRoom.destroyShared()
        chorusRoom = nil
    }
    
    /// 创建合唱房页面
    /// - Returns: 创建合唱房VC
    public func makeCreateChorusViewController(musicDataSource: ChorusMusicService) -> UIViewController {
        let viewModel = makeCreateChorusViewModel()
        let vc =  TRTCCreateChorusViewController.init(viewModel: viewModel, musicDataSource: musicDataSource)
        vc.modalPresentationStyle = .fullScreen
        return vc
    }
    
    /// Chorus
    /// - Parameters:
    ///   - roomInfo: 要进入或者创建的房间参数
    ///   - role: 角色：观众 主播
    /// - Returns: 返回语聊房控制器
    public func makeChorusViewController(roomInfo:ChorusRoomInfo, roleType: ChorusRoleType, toneQuality: ChorusToneQuality = .defaultQuality, musicDataSource: ChorusMusicService) -> UIViewController {
        let viewModel = makeChorusViewModel(roomInfo: roomInfo, roleType: roleType)
        return TRTCChorusViewController.init(viewModel: viewModel,toneQuality: toneQuality, musicDataSource: musicDataSource)
    }
}

extension TRTCChorusEnteryControl: TRTCChorusViewModelFactory {
    func makeChorusViewModel(roomInfo: ChorusRoomInfo, roleType: ChorusRoleType) -> TRTCChorusViewModel {
        let chorusViewModel = TRTCChorusViewModel.init(container: self, roomInfo: roomInfo, roleType: roleType)
        return chorusViewModel
    }
}

extension TRTCChorusEnteryControl: TRTCCreateChorusViewModelFactory {
    func makeCreateChorusViewModel() -> TRTCCreateChorusViewModel {
        return TRTCCreateChorusViewModel.init(dependencyContainer: self)
    }
}

extension TRTCChorusEnteryControl {

    public func createRoom(roomID: String, success: @escaping () -> Void, failed: @escaping (Int32, String) -> Void) {
        if let delegate = self.delegate {
            delegate.chorusCreateRoom(roomId: roomID, success: success, failed: failed)
        }
    }

    public func destroyRoom(roomID: String, success: @escaping () -> Void, failed: @escaping (Int32, String) -> Void) {
        if let delegate = self.delegate {
            delegate.chorusDestroyRoom(roomId: roomID, success: success, failed: failed)
        }
    }
    
    public func generateRTMPPushURL(handler: @escaping ((String, String)?) -> Void) {
        if let delegate = self.delegate {
            return delegate.generateRTMPURL(handler: handler)
        }
    }
}

