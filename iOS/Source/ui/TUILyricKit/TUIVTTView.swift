//
//  TUIVTTView.swift
//  VTTDemo
//
//  Created by adams on 2021/7/16.
//  Copyright © 2022 Tencent. All rights reserved.

import UIKit

class TUIVTTView: UIView {
    
    var vttModel: TUIVttModel? {
        didSet {
            if vttModel != nil {
                lastIndex = -1
            }
            else {
                leftView.lineModel = nil
                rightView.lineModel = nil
            }
        }
    }
    
    var lrcFileUrl: URL? {
        didSet {
            if let url = lrcFileUrl {
                debugPrint("___ set lrc file")
                vttModel = TUIVttParser.parserLocalVTTFile(fileURL: url)
            }
            else {
                debugPrint("___ clear lrc file")
                vttModel = nil
            }
        }
    }
    
    var currentIndex: NSInteger {
        get {
            guard let vttModel = vttModel else {
                return 0
            }
            for (i, model) in vttModel.vttLineModels.enumerated() {
                if model.startTime > currentTime {
                    if i > 0 {
                        return i - 1
                    } else {
                        return i
                    }
                }
            }
            return vttModel.vttLineModels.count - 1
        }
    }
    
    var currentTime: TimeInterval = 0 {
        didSet {
            guard vttModel != nil, lrcFileUrl != nil else {
                return
            }
            setTime(currentTime)
        }
    }
    
    private func setTime(_ time: TimeInterval) {
        guard let vttModel = vttModel else { return }
        let next = currentIndex + 1
        var nextLineModel: TUIVttLineModel?
        if next < vttModel.vttLineModels.count {
            nextLineModel = vttModel.vttLineModels[next]
        }
        let currentlineModel = vttModel.vttLineModels[currentIndex]
        if lastIndex != currentIndex {
            if currentIndex % 2 == 0 {
                leftView.lineModel = currentlineModel
                currentLabel = leftView
                if let nextLineModel = nextLineModel {
                    rightView.isHidden = false
                    rightView.lineModel = nextLineModel
                } else {
                    rightView.isHidden = true
                }
            }
            else {
                rightView.isHidden = false
                rightView.lineModel = currentlineModel
                currentLabel = rightView
                
                let lastlineModel = vttModel.vttLineModels[currentIndex - 1]
                leftView.lineModel = lastlineModel
                let progress = lastlineModel.endTime - lastlineModel.startTime
                leftView.updateProgress(time: progress)
            }
            lastIndex = currentIndex
        }
        
        let progress = time - currentlineModel.startTime
        currentLabel?.updateProgress(time: progress)
    }
    
    var lastIndex: NSInteger = -1
    var leftView: TUIVTTLineView!
    var rightView: TUIVTTLineView!
    var currentLabel: TUIVTTLineView?
    
    init() {
        super.init(frame: .zero)
        setupView()
        backgroundColor = .clear
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

extension TUIVTTView {
    
    private func setupView() {
        leftView = TUIVTTLineView.init(frame: .zero)
        addSubview(leftView)
        leftView.snp.makeConstraints { make in
            make.left.equalToSuperview()
            make.top.equalToSuperview()
        }
        rightView = TUIVTTLineView.init(frame: .zero)
        addSubview(rightView)
        rightView.snp.makeConstraints { make in
            make.right.equalToSuperview()
            make.bottom.equalToSuperview()
            make.top.equalTo(leftView.snp.bottom).offset(10)
        }
    }
    
    public func refreshTime(time: Double) {
        currentTime = time
    }
    
}

class TUIVTTLineView: UIView {
    
    var lineModel: TUIVttLineModel? {
        didSet {
            updateView()
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupView() {
        guard let lineModel = lineModel else { return }
        var lastLabel: TUIVTTLabel?
        for (index,model) in lineModel.charStrArray.enumerated() {
            let characterLabel = TUIVTTLabel.init(frame: .zero)
            characterLabel.characterModel = model
            characterLabel.textAlignment = .left
            characterLabel.normalTextColor = .white
            characterLabel.selectedTextColor = .orange
            characterLabel.font = UIFont(name: "PingFangSC-Semibold", size: 18)
            addSubview(characterLabel)
            if index == 0 {
                characterLabel.snp.makeConstraints { make in
                    make.left.equalToSuperview()
                    make.top.bottom.equalToSuperview()
                }
            } else {
                characterLabel.snp.makeConstraints { make in
                    make.left.equalTo(lastLabel!.snp.right)
                    make.top.bottom.equalTo(lastLabel!)
                    if index == lineModel.charStrArray.count - 1 {
                        make.right.equalToSuperview()
                    }
                }
            }
            lastLabel = characterLabel
        }
    }
    
    func updateView() {
        for subView in subviews {
            guard let vttLabel = subView as? TUIVTTLabel else { continue }
            vttLabel.removeFromSuperview()
        }
        setupView()
    }
    
    func updateProgress(time: Double) {
        guard let lastLabel = subviews.last as? TUIVTTLabel else { return }
        if Int(time) > lastLabel.characterModel.endTime && time < 0 {
            return
        }
        for subView in subviews  {
            guard let vttLabel = subView as? TUIVTTLabel else { continue }
            let mill = time * 1000
            if (mill <= Double(vttLabel.characterModel.endTime)) {
                let current = mill - Double(vttLabel.characterModel.startTime)
                if current >= 0 {
                    let progress = current / Double(vttLabel.characterModel.duration)
                    vttLabel.progress = progress
                    return
                }
            } else {
                vttLabel.progress = 1
            }
        }
    }
}

public class TUIVTTLabel: UIView {
    
    public var font: UIFont? = UIFont(name: "PingFangSC-Semibold", size: 18) {
        didSet {
            textLabel.font = font
            maskLabel.font = font
        }
    }
    
    public var textAlignment: NSTextAlignment = .left {
        didSet {
            textLabel.textAlignment = textAlignment
            maskLabel.textAlignment = textAlignment
        }
    }
    
    public var textColor: UIColor? = .white {
        didSet {
            textLabel.textColor = textColor
        }
    }
    
    public var characterModel: TUIVttCharacterModel = TUIVttCharacterModel.init(startTime: 0, duration: 0, characterStr: "") {
        didSet {
            if oldValue.startTime != characterModel.startTime {
                CATransaction.begin()
                CATransaction.setDisableActions(true)
                maskLayer.bounds = CGRect(x: 0, y: 0, width: 0, height: bounds.height)
                CATransaction.commit()
            }
            textLabel.text = characterModel.characterStr
            maskLabel.text = characterModel.characterStr
            textLabel.sizeToFit()
            maskLabel.sizeToFit()
        }
    }
    
    public var progress: Double = 0 {
        didSet {
            if progress > 0 && progress <= 1 {
                setNeedsDisplay()
            }
        }
    }
    
    public func reset() {
        maskLayer.bounds = CGRect(x: 0, y: 0, width: 0, height: bounds.height)
        progress = 0
    }
    
    var normalTextColor: UIColor? {
        didSet {
            textLabel.textColor = normalTextColor
        }
    }
    
    var selectedTextColor: UIColor = .cyan {
        didSet {
            maskLabel.textColor = selectedTextColor
        }
    }
    
    lazy var textLabel: UILabel = {
        let label = UILabel(frame: bounds)
        label.font = font
        label.text = characterModel.characterStr
        label.textAlignment = textAlignment
        label.textColor = textColor
        return label
    }()
    
    lazy var maskLabel: UILabel = {
        let label = UILabel(frame: bounds)
        label.font = font
        label.text = characterModel.characterStr
        label.textAlignment = textAlignment
        label.textColor = textColor
        label.layer.mask = maskLayer
        backgroundColor = .clear
        return label
    }()
    
    lazy var maskLayer: CALayer = {
        let maskLayer = CALayer()
        maskLayer.anchorPoint = CGPoint(x: 0, y: 0.5)
        maskLayer.backgroundColor = UIColor.white.cgColor
        return maskLayer
    }()
    
    public override func draw(_ rect: CGRect) {
        super.draw(rect)
        maskLayer.position = CGPoint(x: 0, y: bounds.height * 0.5)
        
        if progress == 0 {
            maskLayer.bounds = CGRect(x: 0, y: 0, width: 0, height: bounds.height)
        }
        else {
            maskLayer.bounds = CGRect(x: 0, y: 0, width: maskLabel.bounds.width * CGFloat(progress), height: bounds.height)
        }
    }
    
    private var isViewReady = false
    public override func didMoveToWindow() {
        super.didMoveToWindow()
        if isViewReady {
            return
        }
        isViewReady = true
        
        addSubview(textLabel)
        textLabel.snp.makeConstraints { (make) in
            make.top.bottom.leading.trailing.equalToSuperview()
        }
        
        addSubview(maskLabel)
        maskLabel.snp.makeConstraints { (make) in
            make.top.bottom.leading.trailing.equalToSuperview()
        }
    }
}
