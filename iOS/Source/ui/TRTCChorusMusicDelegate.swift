//
//  TRTCChorusMusicDelegate.swift
//  TUIChorus
//
//  Created by gg on 2021/7/8.
//  Copyright © 2022 Tencent. All rights reserved.

import Foundation

/**
* 歌曲信息回调
*/
public typealias MusicListCallback = (_ list: [ChorusMusicInfo]) -> ()

/**
* 已选列表回调
*/
public typealias MusicSelectedListCallback = (_ list: [ChorusMusicModel]) -> ()

/**
* 下载进度回调
*/
public typealias ProgressCallback = (_ progress: Double) -> ()

/**
* 歌曲管理接口
*/
public protocol ChorusMusicService: AnyObject {
    
//////////////////////////////////////////////////////////
//                 歌曲列表管理
//////////////////////////////////////////////////////////

    /**
    * 获取歌曲列表
    * - parameter page          页码
    * - parameter pageSize      每页的大小
    */
    func chorusGetMusicPage(page: Int, pageSize: Int, callback: @escaping MusicListCallback)
    
    /**
    * 获取已点歌曲列表
    */
    func chorusGetSelectedMusicList(_ callback: @escaping MusicSelectedListCallback)
    
    /**
    * 选择歌曲
    * - parameter musicID   歌曲ID
    */
    func pickMusic(musicID: String, callback: @escaping ActionCallback)
    
    /**
    * 删除歌曲
    * - parameter musicID   歌曲ID
    */
    func deleteMusic(musicID: String, callback: @escaping ActionCallback)
    
    /**
    * 删除某个用户全部已点歌曲
    * - parameter userID   用户ID
    */
    func deleteAllMusic(userID: String, callback: @escaping ActionCallback)
    
    /**
    * 置顶歌曲
    * - parameter musicID   歌曲ID
    */
    func topMusic(musicID: String, callback: @escaping ActionCallback)
    
    /**
    * 切歌
    */
    func nextMusic(callback: @escaping ActionCallback)
    
    /**
    * 歌曲即将播放
    * - parameter musicID   歌曲ID
    */
    func prepareToPlay(musicID: String)
    
    /**
    * 歌曲播放完成
    * - parameter musicID   歌曲ID
    */
    func completePlaying(musicID: String)
    
    /**
    * 退出房间
    */
    func onExitRoom()
    
//////////////////////////////////////////////////////////
//                 预加载管理
//////////////////////////////////////////////////////////
    
    /**
    * 下载歌曲
    * - parameter musicID   歌曲ID
    */
    func downloadMusic(musicID: String, progress: ProgressCallback, complete: @escaping ActionCallback)
    
    /**
    * 下载歌词
    * - parameter musicID   歌曲ID
    */
    func downloadLRC(musicID: String, callback: @escaping ActionCallback)
    
    
//////////////////////////////////////////////////////////
//                 房间信息传递
//////////////////////////////////////////////////////////
    
    /**
    * 设置房间信息
    * - parameter roomInfo   房间信息
    */
    func setRoomInfo(roomInfo: ChorusRoomInfo)
    
    /**
    * 设置回调对象
    * - parameter delegate   代理实现对象
    */
    func setServiceDelegate(_ delegate: ChorusMusicServiceDelegate)
}

/**
* 歌曲管理回调接口
*/
public protocol ChorusMusicServiceDelegate: AnyObject {
    
    /**
    * 歌曲列表更新回调
    * - parameter musicInfoList   已点歌曲列表数组
    */
    func onMusicListChange(musicInfoList: [ChorusMusicModel], reason: Int)
    
    /**
    * 歌词设置回调
    * - parameter musicID   歌曲ID
    */
    func onShouldSetLyric(musicID: String)
    
    /**
    * 歌曲播放回调
    * - parameter musicID   应播放的歌曲
    */
    func onShouldPlay(_ music: ChorusMusicModel)
    
    /**
    * 歌曲停止回调
    * - parameter musicID   应停止播放的歌曲
    */
    func onShouldStopPlay(_ music: ChorusMusicModel)
    
    /**
    * 点歌信息展示回调
    * - parameter musicID   事件发生的歌曲
    */
    func onShouldShowMessage(_ music: ChorusMusicModel)
    
}
