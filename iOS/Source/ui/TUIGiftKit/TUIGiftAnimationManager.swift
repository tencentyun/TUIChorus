//
//  TUIGiftAnimationManager.swift
//  TUIChorus
//
//  Created by adams on 2021/6/25.
//
// 大礼物动画管理类

import Foundation

public protocol TUIGiftAnimationManagerDelegate: NSObject {
    func giftAnimationManager(manager: TUIGiftAnimationManager, giftInfo: TUIGiftInfo?, completion: @escaping () -> Void)
}

public class TUIGiftAnimationManager {
    let kTaskTimeout = 10
    
    class TUIGiftAnimationTask {
        var dealing: Bool = false
        var dealingTime: TimeInterval = 0
        var giftInfo: TUIGiftInfo?  = nil
    }
    
    class TUIGiftAnimationQueue {
        let kQueueDefaultMaxLength = 500
        private var queue: [TUIGiftAnimationTask] = []
        private var lock: NSLock = NSLock.init()
        
        // 入队
        func enqueue(task: TUIGiftAnimationTask) {
            lock.lock()
            if queue.count >= kQueueDefaultMaxLength {
                let task = queue[0]
                if task.dealing {
                    queue.remove(at: 1)
                }
            }
            queue.append(task)
            lock.unlock()
        }
        
        // 出队
        func dequeue() -> TUIGiftAnimationTask? {
            var firstTask: TUIGiftAnimationTask?
            lock.lock()
            if let task = queue.first {
                firstTask = task
                queue.removeFirst()
            }
            lock.unlock()
            return firstTask
        }
        
        // 获取队头元素， 不出队
        func getQueueHeaderTask() -> TUIGiftAnimationTask? {
            return queue.first
        }
        
        // 是否为空
        func isEmpty() -> Bool {
            return queue.count == 0
        }
        
    }
    
    private var queue: TUIGiftAnimationQueue = TUIGiftAnimationQueue.init()
    private var handling: Bool = false
    
    public weak var delegate: TUIGiftAnimationManagerDelegate?
    
    /// 收到礼物动画播放消息
    /// @param giftModel 礼物模型
    public func onRecevie(giftInfo: TUIGiftInfo) {
        let task = TUIGiftAnimationTask.init()
        task.giftInfo = giftInfo
        queue.enqueue(task: task)
        handleTask()
    }
    
    private func handleTask() {
        objc_sync_enter(self)
        if handling || queue.isEmpty() {
            debugPrint(">>>>>>>>>>>> smallyou:正在执行动画或者队列为空 \(queue.isEmpty())")
            return
        }
        handling = true
        objc_sync_exit(self)
        
        if let task = queue.getQueueHeaderTask() {
            if task.dealing {
                if Int(Date.init().timeIntervalSince1970 - task.dealingTime) >= kTaskTimeout {
                    task.dealingTime = 0
                    handleNextTask()
                    return
                }
            } else {
                task.dealing = true
                task.dealingTime = Date.init().timeIntervalSince1970
            }
            notifyDelegateHandle(task: task)
        }
    }
    
    private func handleNextTask() {
        objc_sync_enter(self)
        handling = false
        objc_sync_exit(self)
        _ = queue.dequeue()
        handleTask()
    }
    
    private func notifyDelegateHandle(task: TUIGiftAnimationTask) {
        if !Thread.isMainThread {
            DispatchQueue.main.async {
                self.notifyDelegateHandle(task: task)
            }
            return;
        }
        
        if let delegate = delegate {
            delegate.giftAnimationManager(manager: self, giftInfo: task.giftInfo) { [weak self, weak task] in
                guard let `self` = self, let task = task else { return }
                task.dealing = false
                self.handleNextTask()
            }
            return;
        }
        
        task.dealing = false
        handleNextTask()
    }
    
}
