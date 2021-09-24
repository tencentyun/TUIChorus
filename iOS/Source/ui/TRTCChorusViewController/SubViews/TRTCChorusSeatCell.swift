//
//  TRTCChorusSeatCell.swift
//  TUIChorus
//
//  Created by adams on 2021/8/4.
//  Copyright © 2021 tencent. All rights reserved.
//

import UIKit

enum TRTCChorusSeatCellType {
    case add
    case seat
    case lock
}

class TRTCChorusSeatCell: UICollectionViewCell {
    private var isViewReady: Bool = false
    
    let seatView: TRTCChorusSeatView = {
        let view = TRTCChorusSeatView()
        return view
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else {
            return
        }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
    }
    
    func constructViewHierarchy() {
        contentView.addSubview(seatView)
    }
    
    func activateConstraints() {
        seatView.snp.makeConstraints { (make) in
            make.top.left.bottom.right.equalToSuperview()
        }
    }
    
    func setCell(model: SeatInfoModel, userMuteMap: [String:Bool], seatIndex: Int) {
//        seatView.setSeatInfo(model: model, userMuteMap: userMuteMap, seatIndex: seatIndex)
    }
}
