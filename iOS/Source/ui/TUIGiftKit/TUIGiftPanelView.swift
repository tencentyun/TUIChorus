//
//  TUIGiftPanelView.swift
//  TUIChorus
//
//  Created by adams on 2021/6/24.
//
// 赠送礼物面板

import Foundation
import SnapKit
import TXAppBasic

protocol TUIGiftPanelViewDelegate: NSObject {
    func show(giftModel: TUIGiftModel)
}

class TUIGiftPanelView: UIView {
    
    let giftManager = TUIGiftManager.sharedManager()
    
    lazy var bgView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = .clear
        view.alpha = 0.6
        return view
    }()
    lazy var contentView: UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = UIColor.init(hex:"180B32")
        return view
    }()
    
    lazy var titleLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.textColor = .white
        label.font = UIFont(name: "PingFangSC-Medium", size: 24)
        label.text = .giftTitleText
        return label
    }()
    
    lazy var collectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.itemSize = CGSize(width: 52, height: 76)
        layout.minimumLineSpacing = 20
        layout.minimumInteritemSpacing = 20
        layout.sectionInset = UIEdgeInsets(top: 0, left: 20, bottom: 0, right: 20)
        layout.scrollDirection = .horizontal
        let collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        collectionView.backgroundColor = .clear
        return collectionView
    }()
    
    public var willDismiss: (()->())?
    public var didDismiss: (()->())?
    public weak var parentView: UIView?
    public weak var delegate: TUIGiftPanelViewDelegate?
    
    public override init(frame: CGRect = .zero) {
        super.init(frame: frame)
        contentView.transform = CGAffineTransform(translationX: 0, y: ScreenHeight)
        alpha = 0
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
    }
    
    public func show() {
        UIView.animate(withDuration: 0.3) {
            self.alpha = 1
            self.contentView.transform = .identity
        }
    }
    
    public func dismiss() {
        if let action = willDismiss {
            action()
        }
        UIView.animate(withDuration: 0.3) {
            self.alpha = 0
            self.contentView.transform = CGAffineTransform(translationX: 0, y: ScreenHeight)
        } completion: { (finish) in
            if let action = self.didDismiss {
                action()
            }
            self.removeFromSuperview()
        }
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let point = touches.first?.location(in: self) else {
            return
        }
        if !contentView.frame.contains(point) {
            dismiss()
        }
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        contentView.roundedRect(rect: contentView.bounds, byRoundingCorners: [.topLeft, .topRight], cornerRadii: CGSize(width: 12, height: 12))
    }
    
    func constructViewHierarchy() {
        addSubview(bgView)
        addSubview(contentView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(collectionView)
    }
    
    func activateConstraints() {
        bgView.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
        contentView.snp.makeConstraints { (make) in
            make.leading.trailing.bottom.equalToSuperview()
        }
        titleLabel.snp.makeConstraints { (make) in
            make.leading.equalToSuperview().offset(20)
            make.top.equalToSuperview().offset(32)
        }
        collectionView.snp.makeConstraints { (make) in
            make.top.equalTo(titleLabel.snp.bottom).offset(20)
            make.height.equalTo(76)
            make.leading.trailing.equalToSuperview()
            make.bottom.equalToSuperview().offset(-kDeviceSafeBottomHeight)
        }
    }
    
    func bindInteraction() {
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.register(TUIGiftPanelCell.self, forCellWithReuseIdentifier: TUIGiftPanelCell.reuserIdentify)
        reloadGiftData()
    }
    
    private func reloadGiftData() {
        if giftManager.giftListCache.count == 0 {
            giftManager.requestGiftData { [weak self] status in
                guard let `self` = self else { return }
                if status {
                    DispatchQueue.main.async {
                        self.collectionView.reloadData()
                    }
                }
            }
        }
    }
}

extension TUIGiftPanelView : UICollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let giftModel = giftManager.giftListCache[indexPath.item]
        if let delegate = delegate {
            delegate.show(giftModel: giftModel)
        }
    }
}

extension TUIGiftPanelView : UICollectionViewDataSource {
    
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return giftManager.giftListCache.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: TUIGiftPanelCell.reuserIdentify, for: indexPath)
        if let cell = cell as? TUIGiftPanelCell {
            let giftModel = giftManager.giftListCache[indexPath.item]
            if let url = URL.init(string: giftModel.giftImageUrl) {
                cell.imageView.kf.setImage(with: .network(url))
            }
            cell.titleLabel.text = giftModel.title
        }
        return cell
    }
}

class TUIGiftPanelCell: UICollectionViewCell {
    
    static let reuserIdentify: String = "TUIGiftPanelCell"
    
    lazy var imageView: UIImageView = {
        let imageView = UIImageView(frame: .zero)
        imageView.contentMode = .scaleAspectFill
        return imageView
    }()
    
    lazy var titleLabel: UILabel = {
        let label = UILabel(frame: .zero)
        label.textColor = .white
        label.font = UIFont(name: "PingFangSC-Regular", size: 14)
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.5
        label.textAlignment = .center
        label.alpha = 0.6
        return label
    }()
    
    private var isViewReady = false
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else {
            return
        }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
    }
    private func constructViewHierarchy() {
        contentView.addSubview(imageView)
        contentView.addSubview(titleLabel)
    }
    private func activateConstraints() {
        imageView.snp.makeConstraints { (make) in
            make.leading.trailing.top.equalToSuperview()
            make.size.equalTo(CGSize(width: 52, height: 52))
        }
        titleLabel.snp.makeConstraints { (make) in
            make.top.equalTo(imageView.snp.bottom).offset(4)
            make.leading.trailing.equalToSuperview()
        }
    }
}

fileprivate extension String {
    static let giftTitleText = ChorusLocalize("Demo.TRTC.Chorus.gift")
}
