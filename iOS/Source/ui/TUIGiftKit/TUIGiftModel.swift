//
//  TUIGiftModel.swift
//  TUIChorus
//
//  Created by adams on 2021/6/24.
//  Copyright © 2022 Tencent. All rights reserved.
//  礼物模型及模型管理类

import Foundation

public class TUIGiftManager {
    
    private static let staticInstance: TUIGiftManager = TUIGiftManager.init()
    public static func sharedManager() -> TUIGiftManager { staticInstance }
    private init(){
        requestGiftData { _ in }
    }
    
    private let giftJsonURL = "https://liteav.sdk.qcloud.com/app/res/picture/live/gift/gift_data.json"
    
    public var giftListCache: [TUIGiftModel] = []
    
    public func requestGiftData(completion: @escaping ((Bool) -> Void)) {
        guard let url = URL.init(string: giftJsonURL) else { completion(false); return }
        let urlRequest = URLRequest.init(url: url)
        let dataTask = URLSession.shared.dataTask(with: urlRequest) { [weak self] (data, response, error) in
            guard let `self` = self, let data = data else { completion(false); return }
            guard let giftDic = try? JSONSerialization.jsonObject(with: data, options: .mutableLeaves) as? [String : Any], let giftArray = giftDic["giftList"] else { completion(false); return }
            guard let jsonData = try? JSONSerialization.data(withJSONObject: giftArray, options: .fragmentsAllowed) else { completion(false); return }
            let decoder = JSONDecoder.init()
            if let giftList = try? decoder.decode([TUIGiftModel].self, from: jsonData) {
                self.giftListCache = giftList
                completion(true)
            } else {
                completion(false)
            }
        }
        dataTask.resume()
    }
    
    public func getGiftModel(giftId: String) -> TUIGiftModel? {
        for giftModel in giftListCache where giftModel.giftId == giftId {
            return giftModel
        }
        return nil
    }
    
}

public class TUIGiftModel: Codable {
    
    var giftId: String
    var giftImageUrl: String
    var lottieUrl: String
    var title: String
    var price: Int
    var type: Int
    
    enum CodingKeys: String, CodingKey {
        case giftId
        case giftImageUrl
        case lottieUrl
        case title
        case price
        case type
    }
    
    required public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        do {
            giftId = try container.decode(String.self, forKey: .giftId)
        } catch {
            giftId = ""
        }
        
        do {
            giftImageUrl = try container.decode(String.self, forKey: .giftImageUrl)
        } catch {
            giftImageUrl = ""
        }
        
        do {
            lottieUrl = try container.decode(String.self, forKey: .lottieUrl)
        } catch {
            lottieUrl = ""
        }
        
        do {
            title = try container.decode(String.self, forKey: .title)
        } catch {
            title = ""
        }
        
        do {
            price = try container.decode(Int.self, forKey: .price)
        } catch {
            price = 0
        }
        
        do {
            type = try container.decode(Int.self, forKey: .type)
        } catch {
            type = 0
        }
    }
    
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(giftId, forKey: .giftId)
        try container.encode(giftImageUrl, forKey: .giftImageUrl)
        try container.encode(lottieUrl, forKey: .lottieUrl)
        try container.encode(title, forKey: .title)
        try container.encode(price, forKey: .price)
        try container.encode(type, forKey: .type)
    }
}

public class TUIGiftMsgInfo: Codable {
    var giftId: String
    var sendUser: String                // 礼物发送方昵称
    var sendUserHeadIcon: String        // 礼物发送方头像
    
    enum CodingKeys: String, CodingKey {
        case giftId
        case sendUser
        case sendUserHeadIcon
    }
    
    init(giftId: String, sendUser: String, sendUserHeadIcon: String) {
        self.giftId = giftId
        self.sendUser = sendUser
        self.sendUserHeadIcon = sendUserHeadIcon
    }
    
    required public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        do {
            giftId = try container.decode(String.self, forKey: .giftId)
        } catch {
            giftId = ""
        }
        
        do {
            sendUser = try container.decode(String.self, forKey: .sendUser)
        } catch {
            sendUser = ""
        }
        
        do {
            sendUserHeadIcon = try container.decode(String.self, forKey: .sendUserHeadIcon)
        } catch {
            sendUserHeadIcon = ""
        }
    }
    
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(giftId, forKey: .giftId)
        try container.encode(sendUser, forKey: .sendUser)
        try container.encode(sendUserHeadIcon, forKey: .sendUserHeadIcon)
    }
}

public class TUIGiftInfo {
    var giftModel: TUIGiftModel
    var sendUser: String                // 礼物发送方昵称
    var sendUserHeadIcon: String        // 礼物发送方头像
//    var selected: Bool
//    var context: AnyObject
    
    init(giftModel: TUIGiftModel, sendUser: String, sendUserHeadIcon: String) {
        self.giftModel = giftModel
        self.sendUser = sendUser
        self.sendUserHeadIcon = sendUserHeadIcon
    }
    
}
