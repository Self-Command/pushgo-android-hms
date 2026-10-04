# PushGo for Android — Huawei HMS

基于 [PushGo 官方 Android 源码](https://github.com/AldenClark/pushgo-android) 改造的 HMS 版本，保留原应用界面、数据和消息/事件/对象功能，包名仍为 `io.ethan.pushgo`。

- 本仓库：https://github.com/Self-Command/pushgo-android-hms
- 配套 Gateway：https://github.com/Self-Command/pushgo-gateway-hms

构建配置、签名要求和局域网测试见 [HMS-PUSH.md](HMS-PUSH.md)。AGC 配置与签名文件需要自行提供，不包含在仓库中。
2026-10-05 已在 REDMI Turbo 4 Pro / Android 16 上验证四档优先级、普通消息、超长补拉、去重及事件/对象生命周期。
上游说明和 MIT 许可证保留如下。

PushGo for Android is the official client app for PushGo. It works with PushGo Gateway to receive notifications on Android devices.

## Project Links

- Apple platforms: https://github.com/AldenClark/pushgo
- Gateway: https://github.com/AldenClark/pushgo-gateway
- Android app (this repo): https://github.com/AldenClark/pushgo-android

## Requirements

- Android 9+ (minSdk 28)

---

# PushGo Android（中文）

PushGo Android 是 PushGo 的官方客户端应用，可配合 PushGo Gateway 在 Android 设备上接收通知。

## 项目链接

- Apple 平台：https://github.com/AldenClark/pushgo
- 网关：https://github.com/AldenClark/pushgo-gateway
- Android App（本仓库）：https://github.com/AldenClark/pushgo-android

## 环境要求

- Android 9+（minSdk 28）
