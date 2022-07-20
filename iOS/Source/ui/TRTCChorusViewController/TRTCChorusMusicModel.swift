//
//  TRTCChorusMusicModel.swift
//  TUIChorus
//
//  Created by adams on 2021/8/2.
//  Copyright © 2022 Tencent. All rights reserved.

import Foundation

public class ChorusMusicInfo: NSObject {
    
    public let coverUrl: String
    public let musicName: String
    public let singer: String
    public var contentUrl: String
    public var lrcUrl: String
    public let musicID: Int32
    
    public init(title: String, coverUrl: String, author: String, path: String, lrcPath: String, musicID: Int32) {
        self.coverUrl = coverUrl
        self.musicName = title
        self.singer = author
        self.contentUrl = path
        self.lrcUrl = lrcPath
        self.musicID = musicID
        super.init()
    }
}

public class ChorusMusicModel: NSObject {
    
    public let music: ChorusMusicInfo
    public var isSelected: Bool
    
    public var seatIndex: Int = -1
    public var bookUserName: String = ""
    public var bookUserID: String = ""
    public var bookUserAvatar: String = ""
    
    public var action: ((_ model: ChorusMusicModel) -> (Bool))?
    
    public var coverUrl: String {
        return music.coverUrl
    }
    public var musicName: String {
        return music.musicName
    }
    public var singer: String {
        return music.singer
    }
    public var contentUrl: String {
        return music.contentUrl
    }
    public var lrcUrl: String {
        return music.lrcUrl
    }
    public var musicID: Int32 {
        return music.musicID
    }
    
    public init(sourceModel: ChorusMusicInfo, isSelected: Bool = false, action: ((_ model: ChorusMusicModel) -> (Bool))? = nil) {
        self.isSelected = isSelected
        self.action = action
        self.music = sourceModel
        super.init()
    }
    
    public var jsonDic: [String:Any] {
        var dic: Dictionary<String, Any> = [:]
        dic["musicId"] = String(musicID)
        dic["musicName"] = musicName
        dic["singer"] = singer
        dic["lrcUrl"] = lrcUrl
        dic["contentUrl"] = contentUrl
        dic["coverUrl"] = coverUrl
        dic["bookUser"] = bookUserID
        return dic
    }
    
    public static func json(_ json: [String:Any]) -> ChorusMusicModel? {
        guard let musicIDStr = json["musicId"] as? String, let musicID = Int32(musicIDStr) else { return nil }
        guard let title = json["musicName"] as? String else { return nil }
        guard let author = json["singer"] as? String else { return nil }
        guard let lrc = json["lrcUrl"] as? String else { return nil }
        guard let path = json["contentUrl"] as? String else { return nil }
        guard let cover = json["coverUrl"] as? String else { return nil }
        let info = ChorusMusicInfo(title: title, coverUrl: cover, author: author, path: path, lrcPath: lrc, musicID: musicID)
        let model = ChorusMusicModel(sourceModel: info)
        guard let singer = json["bookUser"] as? String else { return nil }
        model.bookUserID = singer
        return model
    }
    
    public func reset() {
        isSelected = false
        seatIndex = -1
        bookUserName = ""
        bookUserID = ""
        bookUserAvatar = ""
    }
}
