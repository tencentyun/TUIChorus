//
//  TRTCChorusSoundEffectViewModel.swift
//  TUIChorus
//
//  Created by adams on 2021/8/4.
//  Copyright © 2021 Tencent. All rights reserved.
//

import Foundation

class TRTCAudioEffectCellModel: NSObject {
    var actionID: Int = 0
    var title: String = ""
    var icon: UIImage?
    var selectIcon: UIImage?
    var selected: Bool = false
    var action: (()->())?
}

protocol TRTCChorusSoundEffectViewResponder: NSObject {
    func bgmOnPrepareToPlay(musicID: Int32)
    func bgmOnPlaying(musicID:Int32, current: Double, total: Double)
    func bgmOnCompletePlaying()
    func onSelectedMusicListChanged()
    func onMusicListChanged()
    func onManageSongBtnClick()
    func showStartAnimationAndPlay(startDelay: Int)
}

class TRTCChorusSoundEffectViewModel: NSObject {
    
    weak var viewResponder: TRTCChorusSoundEffectViewResponder?
    
    weak var viewModel : TRTCChorusViewModel?
    
    init(_ model: TRTCChorusViewModel) {
        self.viewModel = model
        super.init()
        reloadMusicList()
        reloadSelectedMusicList()
    }
    
    lazy var manager : TXAudioEffectManager? = {
        return viewModel?.chorusRoom.getAudioEffectManager()
    }()
    
    var currentMusicVolum: Int = 100
    var currentVocalVolume: Int = 100
    var currentPitchVolum: Double = 0
    
    var bgmID : Int32 = 0
    
    public func setVolume(music: Int) {
        currentMusicVolum = music
        guard let manager = manager else {
            return
        }
        if bgmID != 0 {
            manager.setMusicPlayoutVolume(bgmID, volume: music)
            manager.setMusicPublishVolume(bgmID, volume: music)
        }
    }
    
    public func setEarMonitor(_ enable: Bool) {
        guard let manager = manager else {
            return
        }
        manager.enableVoiceEarMonitor(enable)
    }
    
    public func setVolume(person: Int) {
        currentVocalVolume = person
        guard let manager = manager else {
            return
        }
        manager.setVoiceVolume(person)
    }
    
    public func setPitch(person: Double) {
        currentPitchVolum = person
        guard let manager = manager else {
            return
        }
        if bgmID != 0 {
            manager.setMusicPitch(bgmID, pitch: person)
        }
    }
    
    // MARK: - Music
    let loadPageSize = 10
    var currentPlayingModel: ChorusMusicModel?
    
    lazy var musicList: [ChorusMusicModel] = []
    lazy var musicSelectedList: [ChorusMusicModel] = []
    
    lazy var listAction: ((_ model: ChorusMusicModel) -> (Bool)) = { [weak self] model in
        guard let `self` = self else { return false }
        switch self.viewModel?.userType {
        case .anchor:
            self.viewModel?.musicDataSource?.pickMusic(musicID: String(model.musicID), callback: { (code, msg) in
                
            })
            return true
        default:
            self.viewModel?.viewResponder?.showToast(message: .notInSeatText)
            return false
        }
    }
    
    lazy var selectedAction: ((_ model: ChorusMusicModel) -> (Bool)) = { [weak self] model in
        guard let `self` = self else { return false }
        guard let index = self.musicSelectedList.firstIndex(of: model) else {
            return false
        }
        if index == 0 {
            self.viewModel?.musicDataSource?.nextMusic(callback: { [weak self] (code, msg) in
                guard let `self` = self else { return }
                if code == 0 {
                    if let viewResponder = self.viewResponder {
                        viewResponder.bgmOnCompletePlaying()
                    }
                }
            })
        } else {
            self.viewModel?.musicDataSource?.topMusic(musicID: String(model.musicID), callback: { (code, msg) in
                
            })
        }
        return false
    }
    
    func resetToNormalState(_ model: ChorusMusicModel) {
        model.isSelected = false
        model.seatIndex = -1
        model.bookUserName = ""
        model.bookUserID = ""
        model.action = listAction
    }
    
    func reloadMusicList() {
        viewModel?.musicDataSource?.chorusGetMusicPage(page: 0, pageSize: loadPageSize, callback: { [weak self] (list) in
            guard let `self` = self else { return }
            self.musicList.removeAll()
            for sourceModel in list {
                let model = ChorusMusicModel(sourceModel: sourceModel, action: self.listAction)
                self.musicList.append(model)
            }
            self.viewResponder?.onMusicListChanged()
        })
    }
    func loadNextPageList() {
        
    }
    func reloadSelectedMusicList() {
        viewModel?.musicDataSource?.chorusGetSelectedMusicList({ [weak self] (list) in
            guard let `self` = self else { return }
            self.musicSelectedList = list
            self.musicSelectedList.forEach { (music) in
                music.action = self.selectedAction
            }
            self.viewResponder?.onSelectedMusicListChanged()
        })
    }
    
    func playMusic(_ model: ChorusMusicModel) {
        currentPlayingModel = model
        bgmID = model.musicID
        setCurrentMusicEffect()
        viewModel?.chorusRoom.startPlayMusic(musicID: model.musicID, url: model.contentUrl)
    }
    
    func stopPlay() {
        currentPlayingModel = nil
        bgmID = 0
        viewModel?.chorusRoom.stopPlayMusic()
    }
    
    func pausePlay() {
        viewModel?.chorusRoom.pausePlayMusic()
    }
    
    func resumePlay() {
        viewModel?.chorusRoom.resumePlayMusic()
    }
    
    func clearStatus() {
        currentPlayingModel = nil
        if bgmID != 0 {
            setPitch(person: 0)
            stopPlay()
            bgmID = 0
        }
        setVolume(music: 100)
        setVolume(person: 100)
        
    }
    
    func setCurrentMusicEffect() {
        setVolume(music: currentMusicVolum)
        setVolume(person: currentVocalVolume)
        setPitch(person: currentPitchVolum)
        self.manager?.setVoiceReverbType(currentReverb)
        self.manager?.setVoiceChangerType(currentChangerType)
    }
    
    // MARK: - Voice change and reverb
    var currentChangerType : TXVoiceChangeType = ._0
    var currentReverb : TXVoiceReverbType = ._0
    
    lazy var reverbDataSource: [TRTCAudioEffectCellModel] = {
        var res: [TRTCAudioEffectCellModel] = []
        let titleArray = [
            chorusLocalize("ASKit.MenuItem.No effect"),
            chorusLocalize("ASKit.MenuItem.Karaoke room"),
            chorusLocalize("ASKit.MenuItem.Metallic"),
            chorusLocalize("ASKit.MenuItem.Deep"),
            chorusLocalize("ASKit.MenuItem.Resonant"),
            ]
        let iconNameArray = [
            "originState_nor",
            "Reverb_Chorus_nor",
            "Reverb_jinshu_nor",
            "Reverb_dichen_nor",
            "Reverb_hongliang_nor",
        ]
        let iconSelectedNameArray = [
            "originState_sel",
            "Reverb_Chorus_sel",
            "Reverb_jinshu_sel",
            "Reverb_dichen_sel",
            "Reverb_hongliang_sel",
        ]
        for index in 0..<titleArray.count {
            let title = titleArray[index]
            let normalIconName = iconNameArray[index]
            let selectIconName = iconSelectedNameArray[index]
            
            let model = TRTCAudioEffectCellModel()
            model.actionID = index
            model.title = title
            model.selected = title == chorusLocalize("ASKit.MenuItem.No effect")
            model.icon = UIImage(named: normalIconName, in: chorusBundle(), compatibleWith: nil)
            model.selectIcon = UIImage(named: selectIconName, in: chorusBundle(), compatibleWith: nil)
            model.action = { [weak self] in
                guard let `self` = self else { return }
                let type = self.switch2ReverbType(index)
                self.manager?.setVoiceReverbType(type)
                self.currentReverb = type
            }
            if model.icon != nil {
                res.append(model)
            }
        }
        return res
    }()
    
    lazy var voiceChangeDataSource: [TRTCAudioEffectCellModel] = {
        var res: [TRTCAudioEffectCellModel] = []
        
        let titleArray =
            [chorusLocalize("ASKit.MenuItem.Original"),
             chorusLocalize("ASKit.MenuItem.Naughty boy"),
             chorusLocalize("ASKit.MenuItem.Little girl"),
             chorusLocalize("ASKit.MenuItem.Middle-aged man"),
             chorusLocalize("ASKit.MenuItem.Ethereal voice"),
             ]
        
        let iconNameArray = [
            "originState_nor",
            "voiceChange_xionghaizi_nor",
            "voiceChange_loli_nor",
            "voiceChange_dashu_nor",
            "voiceChange_kongling_nor",
        ]
        
        let iconSelectedNameArray = [
            "originState_sel",
            "voiceChange_xionghaizi_sel",
            "voiceChange_loli_sel",
            "voiceChange_dashu_sel",
            "voiceChange_kongling_sel",
            ]
        
        for index in 0..<titleArray.count {
            let title = titleArray[index]
            let normalIconName = iconNameArray[index]
            let selectedIconName = iconSelectedNameArray[index]
            let model = TRTCAudioEffectCellModel()
            model.title = title
            model.actionID = index
            model.selected = title == chorusLocalize("ASKit.MenuItem.Original")
            model.icon = UIImage(named: normalIconName, in: chorusBundle(), compatibleWith: nil)
            model.selectIcon = UIImage(named: selectedIconName, in: chorusBundle(), compatibleWith: nil)
            model.action = { [weak self] in
                guard let `self` = self else { return }
                let type = self.switch2VoiceChangeType(index)
                self.manager?.setVoiceChangerType(type)
                self.currentChangerType = type
            }
            if model.icon != nil {
                res.append(model)
            }
        }
        return res
    }()
    
    func switch2VoiceChangeType(_ index: Int) -> TXVoiceChangeType {
        switch index {
        case 0:
            return ._0
        case 1:
            return ._1
        case 2:
            return ._2
        case 3:
            return ._3
        case 4:
            return ._11
        default:
            return ._0
        }
    }
    
    func switch2ReverbType(_ index: Int) -> TXVoiceReverbType {
        switch index {
        case 0:
            return ._0
        case 1:
            return ._1
        case 2:
            return ._6
        case 3:
            return ._4
        case 4:
            return ._5
        default:
            return ._0
        }
    }
}

/// MARK: - internationalization string
fileprivate extension String {
    static let musicTitle1Text = chorusLocalize("Demo.TRTC.Chorus.musicname1")
    static let musicTitle2Text = chorusLocalize("Demo.TRTC.Chorus.musicname2")
    static let musicTitle3Text = chorusLocalize("Demo.TRTC.Chorus.musicname3")
    static let notInSeatText = chorusLocalize("Demo.TRTC.Chorus.onlyanchorcanoperation")
}
