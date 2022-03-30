# TUIChorus Android 示例工程快速跑通

本文档主要介绍如何快速跑通 TUIChorus 示例工程，体验实时在线合唱场景，包括低延时合唱、麦位管理、收发礼物、文字聊天等 TRTC 在合唱场景下的相关能力。更详细的 TUIChorus 组件接入流程，请点击腾讯云官网文档： [**TUIChorus 组件 Android 接入说明** ](https://cloud.tencent.com/document/product/647/61859)...

## 目录结构
```
TUIChorus
├─ App          // 主面板，合唱场景入口
├─ Debug        // 调试相关
└─ Source       // 合唱业务逻辑
```

## 环境准备
- 最低兼容 Android 4.2（SDK API Level 17），建议使用 Android 5.0 （SDK API Level 21）及以上版本
- Android Studio 3.5及以上版本
  
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
2. 找到并打开 `Android/Debug/src/main/java/com/tencent/liteav/debug/GenerateTestUserSig.java` 文件。
3. 配置 `GenerateTestUserSig.java` 文件中的相关参数：
	<img src="https://qcloudimg.tencent-cloud.cn/raw/624da9c6c806f166d9f552043fa93e7c.png" width="900">
	- SDKAPPID：默认为占位符（PLACEHOLDER），请设置为第一步中记录下的 SDKAppID。
	- SECRETKEY：默认为占位符（PLACEHOLDER），请设置为第一步中记录下的秘钥信息。
	- URL_FETCH_PUSH_URL：默认为占位符（PLACEHOLDER），请设置为第二步中记录下的推拉流地址。

### 第四步：编译运行
使用 Android Studio 打开源码目录 `TUIChorus/Android`，待Android Studio工程同步完成后，连接真机单击 **运行按钮** 即可开始体验本APP。

### 第五步：示例体验

Tips：TUIChorus 使用体验，需要两台设备，如果用户A/B分别代表两台不同的设备：

**设备 A（userId：258）**

步骤1、输入用户名(<font color=red>请确保用户名唯一性，不能与其他用户重复</font>)；

步骤2、点击创建房间；

步骤3、输入房间主题，点击实时合唱。

| 步骤1 | 步骤2 | 步骤3 |
|---------|---------|---------|
|<img src="https://main.qcloudimg.com/raw/5dad8ffa1b862e8b8f640748ab6ef813.png" width="320"/>|<img src="https://main.qcloudimg.com/raw/114200ce12aa05903ca2e0c1b1454389.png" width="320"/>|<img src="https://qcloudimg.tencent-cloud.cn/raw/95d14bef5d5ab78f817456ee345098f6.png" width="320"/>|

**设备 B（userId：369）**

步骤1、输入用户名(<font color=red>请确保用户名唯一性，不能与其他用户重复</font>)；

步骤2、输入设备 A 创建的房间号，点击进入房间。

<font color=red>请注意，房间号在设备 A 的房间顶部查看。</font>

| 步骤1 | 步骤2 | 注意 |
|---------|---------|---------|
|<img src="https://main.qcloudimg.com/raw/23142bd7393882bf0bad4301d192401a.png" width="320"/>|<img src="https://main.qcloudimg.com/raw/5676a2cde2d284c0518d8a1f819a3f36.png" width="320"/>|<img src="https://main.qcloudimg.com/raw/912aa8ca0d2b2b9f816e167c46550a24.png" width="320"/>|


## 常见问题

- [TUI 场景化解决方案常见问题](https://cloud.tencent.com/developer/article/1952880)
- 欢迎加入 QQ 群：592465424，进行技术交流和反馈~
