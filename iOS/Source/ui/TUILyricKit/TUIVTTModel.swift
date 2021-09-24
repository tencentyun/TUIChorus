//
//  TUIVTTModel.swift
//  VTTDemo
//
//  Created by adams on 2021/7/16.
//

import Foundation

public struct TUIVttModel {
    var vttLineModels: [TUIVttLineModel]
}

public struct TUIVttLineModel {
    var charStrArray: [TUIVttCharacterModel]
    var startTime: Double
    var endTime: Double
}

public struct TUIVttCharacterModel {
    let startTime: Int
    let duration: Int
    var endTime: Int {
        return startTime + duration
    }
    let characterStr: String
}

public class TUIVttParser: NSObject {
    static var isVtt: Bool = false
    static var lineVttModel: TUIVttLineModel = TUIVttLineModel.init(charStrArray: [], startTime: 0, endTime: 0)
    
    static func parserLocalVTTFile(fileURL: URL) -> TUIVttModel? {
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            return nil
        }
        guard let vttStr = try? String(contentsOfFile: fileURL.path) else {
            return nil
        }
        let arrayVtt = vttStr.components(separatedBy: "\n")
        var vttModel = TUIVttModel.init(vttLineModels: [])
        for str in arrayVtt {
            if str.contains("-->") {
                let timeArray = str.replacingOccurrences(of: " ", with: "").components(separatedBy: "-->")
                if timeArray.count == 2 {
                    let startTimeArray = timeArray[0].components(separatedBy: ":")
                    let endTimeArray = timeArray[1].components(separatedBy: ":")
                    guard let smin = Double(startTimeArray[1]), let ssec = Double(startTimeArray[2]) else { continue }
                    guard let emin = Double(endTimeArray[1]), let esec = Double(endTimeArray[2]) else { continue }
                    let startTime = smin * 60 + ssec
                    let endTime = emin * 60 + esec
                    lineVttModel.startTime = startTime
                    lineVttModel.endTime = endTime
                    isVtt = true
                }
            }
            
            if str.hasPrefix("<") {
                let lineVttArray = str.components(separatedBy: "<")
                for lineStr in lineVttArray {
                    if lineStr.count > 0 {
                        let line = lineStr.replacingOccurrences(of: ",0>", with: ",")
                        let characterArray = line.components(separatedBy: ",")
                        guard characterArray.count == 3 else { continue }
                        guard let startTime = Int(characterArray[0]), let duration = Int(characterArray[1]) else {
                            continue
                        }
                        let characterStr = characterArray[2]
                        if isVtt {
                            lineVttModel.charStrArray.append(TUIVttCharacterModel.init(startTime: startTime, duration: duration, characterStr: characterStr))
                        }
                    }
                }
                isVtt = false
            }
            
            if !isVtt && lineVttModel.charStrArray.count > 0 {
                let lineVttModelTemp = TUIVttLineModel.init(charStrArray: lineVttModel.charStrArray, startTime: lineVttModel.startTime, endTime: lineVttModel.endTime)
                vttModel.vttLineModels.append(lineVttModelTemp)
                lineVttModel.charStrArray.removeAll()
                lineVttModel.startTime = 0
            }
        }
        
        if vttModel.vttLineModels.count == 0 {
            return nil
        }
        return vttModel
    }
    
}

fileprivate extension String {
    static let noLyricText = "Demo.TRTC.Chorus.nolyricfound"
}
