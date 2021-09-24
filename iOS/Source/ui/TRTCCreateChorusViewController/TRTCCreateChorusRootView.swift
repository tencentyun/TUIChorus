//
//  TRTCCreateChorusRootView.swift
//  TUIChorus
//
//  Created by adams on 2021/7/14.
//

import UIKit
import TXAppBasic

class TRTCCreateChorusRootView: UIView {
    
    public var screenShot : UIView?
    
    private let bgView : UIView = {
        let bg = UIView(frame: .zero)
        bg.backgroundColor = .black
        bg.alpha = 0.6
        return bg
    }()
    
    private let contentView : UIView = {
        let view = UIView(frame: .zero)
        view.backgroundColor = .white
        return view
    } ()
    
    private let titleLabel : UILabel = {
        let label = UILabel(frame: .zero)
        label.font = UIFont(name: "PingFangSC-Medium", size: 24)
        label.textColor = .black
        label.textAlignment = .left
        label.text = .titleText
        return label
    }()
    
    private lazy var textView : UITextView = {
        let textView = UITextView(frame: .zero)
        textView.font = UIFont(name: "PingFangSC-Regular", size: 16)
        textView.textContainerInset = UIEdgeInsets(top: 20, left: 20, bottom: 20, right: 20)
        textView.text = LocalizeReplaceXX(.defaultCreateText, viewModel.userName).subString(toByteLength: createRoomTextMaxByteLength)
        textView.textColor = .black
        textView.layer.cornerRadius = 20
        textView.backgroundColor = UIColor(hex: "F4F5F9")
        return textView
    }()
    
    private let createBtn : UIButton = {
        let btn = UIButton(type: .custom)
        btn.setTitle(.createText, for: .normal)
        btn.titleLabel?.textColor = .white
        btn.titleLabel?.font = UIFont(name: "PingFangSC-Medium", size: 18)
        btn.isEnabled = true
        btn.clipsToBounds = true
        return btn
    }()
    
    private func createScreenShot() {
        guard let view = screenShot else {
            return
        }
        insertSubview(view, belowSubview: bgView)
        view.snp.makeConstraints { (make) in
            make.edges.equalToSuperview()
        }
    }
    
    private let viewModel: TRTCCreateChorusViewModel
    
    init(viewModel: TRTCCreateChorusViewModel, screenShot: UIView?, frame: CGRect = .zero) {
        self.viewModel = viewModel
        self.screenShot = screenShot
        super.init(frame: frame)
        bindInteraction()
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardFrameChange(noti:)), name: UIResponder.keyboardWillChangeFrameNotification, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    @objc
    func keyboardFrameChange(noti : Notification) {
        guard let info = noti.userInfo else {
            return
        }
        guard let value = info[UIResponder.keyboardFrameEndUserInfoKey], value is CGRect else {
            return
        }
        let rect = value as! CGRect
        transform = CGAffineTransform(translationX: 0, y: -ScreenHeight+rect.minY)
    }
    
    var isViewReady = false
    
    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard !isViewReady else {
            return
        }
        isViewReady = true
        constructViewHierarchy()
        activateConstraints()
        bindInteraction()
        createScreenShot()
    }
    
    override func draw(_ rect: CGRect) {
        super.draw(rect)
        createBtn.layer.cornerRadius = createBtn.frame.height*0.5
        contentView.roundedRect(rect: contentView.bounds, byRoundingCorners: [.topLeft, .topRight], cornerRadii: CGSize(width: 12, height: 12))
        createBtn.gradient(colors: [UIColor(hex: "FF88DD")!.cgColor, UIColor(hex: "7D00BD")!.cgColor])
    }
    
    private func constructViewHierarchy() {
        addSubview(bgView)
        addSubview(contentView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(textView)
        contentView.addSubview(createBtn)
    }
    
    private func activateConstraints() {
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
        textView.snp.makeConstraints { (make) in
            make.leading.equalToSuperview().offset(20)
            make.trailing.equalToSuperview().offset(-20)
            make.top.equalTo(titleLabel.snp.bottom).offset(20)
            make.height.equalTo(176)
        }
        createBtn.snp.makeConstraints { (make) in
            make.centerX.equalToSuperview()
            make.top.equalTo(textView.snp.bottom).offset(32)
            make.height.equalTo(52)
            make.width.equalTo(160)
            make.bottom.equalToSuperview().offset(-54)
        }
    }
    
    private func bindInteraction() {
        createBtn.addTarget(self, action: #selector(createBtnClick), for: .touchUpInside)
        textView.delegate = self
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let point = touches.first?.location(in: self) else {
            return
        }
        if contentView.frame.contains(point) {
            textView.endEdit()
        }
        else {
            textView.resignFirstResponder()
            viewModel.navigationPop()
        }
    }
    
    @objc
    func createBtnClick() {
        if textView.isFirstResponder {
            textView.resignFirstResponder()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                guard let `self` = self else { return }
                self.enterRoom()
            }
        }
        else {
            enterRoom()
        }
    }
    
    private func enterRoom() {
        if textView.text == String.placeholderTitleText {
            viewModel.roomName = LocalizeReplaceXX(.defaultCreateText, viewModel.userName)
        }
        else {
            viewModel.roomName = textView.text
        }
        viewModel.createRoom()
    }
}


extension TRTCCreateChorusRootView : UITextViewDelegate {
    func textViewShouldBeginEditing(_ textView: UITextView) -> Bool {
        textView.beganEdit()
        return true
    }
    func textViewDidBeginEditing(_ textView: UITextView) {
        textView.becomeFirstResponder()
    }
    func textViewDidEndEditing(_ textView: UITextView) {
        textView.endEdit()
        createBtn.isEnabled = textView.text != String.placeholderTitleText
    }
    func textViewDidChange(_ textView: UITextView) {
        createBtn.isEnabled = textView.text != ""
        textView.text = textView.text.subString(fromByteLength: 30)
    }
}

extension String {
    func subString(fromByteLength: Int) -> String {
        guard let data = data(using: .utf8) else {
            return ""
        }
        if data.count > fromByteLength {
            guard let str = String(data: data[0..<fromByteLength], encoding: .utf8) else {
                guard let str = String(data: data[0..<(fromByteLength - 1)], encoding: .utf8) else {
                    return ""
                }
                return str
            }
            return str
        }
        else {
            return self
        }
    }
}

extension UITextView {
    func beganEdit() {
        if self.text == String.placeholderTitleText {
            self.text = ""
            self.textColor = .black
        }
    }
    func endEdit() {
        if self.text == "" {
            self.text = .placeholderTitleText
            self.textColor = UIColor(hex: "BBBBBB")
        }
        self.resignFirstResponder()
    }
}

/// MARK: - internationalization string
fileprivate extension String {
    static let titleText = ChorusLocalize("Demo.TRTC.Chorus.roomsubject")
    static let placeholderTitleText = ChorusLocalize("Demo.TRTC.Chorus.enterroomsubject")
    static let createText = ChorusLocalize("Demo.TRTC.Chorus.join")
    static let defaultCreateText = ChorusLocalize("Demo.TRTC.Chorus.xxxsroom")
}
