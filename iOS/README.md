# TUIChorus iOS 示例工程快速跑通

本文档主要介绍如何快速跑通 TUIChorus 示例工程，体验实时在线合唱场景，包括低延时合唱、麦位管理、收发礼物、文字聊天等 TRTC 在合唱场景下的相关能力。更详细的 TUIChorus 组件接入流程，请点击腾讯云官网文档： [**TUIChorus 组件 iOS 接入说明** ](https://cloud.tencent.com/document/product/647/61858)...


## 目录结构

```
TUIChorus
├─ Example                // 工程模块，主要提供 TUIChorus 的测试页面
├─ Resources              // Chorus 功能所需的图片、国际化字符串资源文件夹
├─ Source                 // Chorus 核心业务逻辑代码文件夹
├─ TXAppBasic             // 工程依赖的基础组件
└─ TUIChorus.podspec      // TUIChorus 组件 pod 接入文件
```

## 环境准备
- Xcode 11.0及以上版本
- 最低支持系统：iOS 13.0
- 请确保您的项目已设置有效的开发者签名

## 运行示例

### 第一步：创建TRTC的应用
1. 一键进入腾讯云实时音视频控制台的[应用管理](https://console.cloud.tencent.com/trtc/app)界面，选择创建应用，输入应用名称，例如 `TUIKitDemo` ，单击 **创建**；
2. 点击对应应用条目后的**应用信息**，具体位置如下图所示：
    <img src="https://qcloudimg.tencent-cloud.cn/raw/62f58d310dde3de2d765e9a460b8676a.png" width="900">
3. 进入应用信息后，按下图操作，记录SDKAppID和密钥：
    <img src="https://qcloudimg.tencent-cloud.cn/raw/bea06852e22a33c77cb41d287cac25db.png" width="900">

>! 本功能同时使用了腾讯云 [实时音视频 TRTC](https://cloud.tencent.com/document/product/647/16788) 和 [即时通信 IM](https://cloud.tencent.com/document/product/269) 两个基础 PaaS 服务，开通实时音视频后会同步开通即时通信 IM 服务。 即时通信 IM 属于增值服务，详细计费规则请参见 [即时通信 IM 价格说明](https://cloud.tencent.com/document/product/269/11673)。

### 第二步：生成推拉流地址
TUIChorus 合唱场景您需要开通推拉流，用于主唱端推流，听众端拉流。
请参照 [推拉流URL](https://cloud.tencent.com/document/product/454/7915) 文档，生成自己的推拉流地址并保存。

[](id:ui.step2)
### 第三步：下载源码，配置工程
1. 克隆或者直接下载此仓库源码，**欢迎 Star**，感谢~~
2. 找到并打开 `Example/Debug/GenerateTestUserSig.swift` 文件。
3. 配置 `GenerateTestUserSig.swift` 文件中的相关参数：
    <img src="https://liteav.sdk.qcloud.com/doc/res/trtc/picture/zh-cn/sdkappid_secretkey_ios.png" width="900">
    - SDKAPPID：默认为占位符（PLACEHOLDER），请设置为第一步中记录下的 SDKAppID。
    - SECRETKEY：默认为占位符（PLACEHOLDER），请设置为第一步中记录下的秘钥信息。

### 第四步：编译运行

1. 打开终端进入到`Example/Podfile`文件所在目录下，执行`pod install`命令。
2. 使用 Xcode（11.0及以上的版本）打开源码工程 `Example/TUIChorusApp.xcworkspace`，单击 **运行按钮** 即可开始体验本APP。

### 第五步：示例体验

Tips：TUIChorus 使用体验，需要两台设备，如果用户A/B分别代表两台不同的设备：

**设备 A（userId：111）**

步骤1、输入用户名(<font color=red>请确保用户名唯一性，不能与其他用户重复</font>)；

步骤2、点击创建房间；

步骤3、输入房间主题，点击实时合唱；

| 步骤1 | 步骤2 | 步骤3 | 
|---------|---------|---------|
|<img src="https://main.qcloudimg.com/raw/7507b977036da789712ad6638ac3180c.png" width="320"/>|<img src="https://main.qcloudimg.com/raw/40e7560dd7ddf856534022902dc1e8b5.png" width="320"/>|<img src="https://qcloudimg.tencent-cloud.cn/raw/f45a87624d99a47220001c8c4b87f533.png" width="320"/>|



**设备 B（userId：222）**

步骤1、输入用户名(<font color=red>请确保用户名唯一性，不能与其他用户重复</font>)：

步骤2、输入用户 A 创建的房间号，点击进入房间

<font color=red>请注意，房间号在用户 A 的房间顶部查看，如下图示：</font>

| 步骤1 | 步骤2 | 注意 | 
|---------|---------|---------|
|<img src="https://main.qcloudimg.com/raw/573cc79ddf42e42eebd19c98757d64b1.png" width="320"/>|<img src="https://main.qcloudimg.com/raw/ff4f8bf3a52d922e745387c36ef074d5.png" width="320"/>|<img src="https://main.qcloudimg.com/raw/fe8f0897fea336997cf5c51ead9cc6c7.png" width="320"/>|


## 常见问题

- [TUI 场景化解决方案常见问题](https://cloud.tencent.com/developer/article/1952880)
- 欢迎加入 QQ 群：592465424，进行技术交流和反馈~
