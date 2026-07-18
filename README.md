# 小科科普助手 APK

## 项目结构

```
smart-speaker-app/
├── build.gradle.kts           # 根构建文件
├── settings.gradle.kts        # 项目设置
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── build.sh                   # 编译脚本
└── app/
    ├── build.gradle.kts       # 模块构建
    ├── proguard-rules.pro     # 混淆规则
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/xiaoke/speaker/
        │   └── MainActivity.java   # 主 Activity
        └── res/
            ├── layout/activity_main.xml  # 布局
            ├── values/themes.xml
            ├── values/strings.xml
            └── xml/network_security_config.xml
```

## 工作原理

APK 是一个 WebView 壳，加载 `https://192.168.3.12:3443`（电脑上的服务）。

通过 `NativeBridge` (JavaScriptInterface) 桥接原生能力：

| 原生能力 | JS 接口 | 对应 `window.__onXxx` 回调 |
|:---------|:--------|:------------------------|
| 语音识别 (SpeechRecognizer) | `NativeBridge.startSpeechRec()` | `__onRecResult(text)` / `__onRecError(err)` |
| 语音合成 (TextToSpeech) | `NativeBridge.speak(text)` / `NativeBridge.stopSpeaking()` | `__onTTSStatus(status)` |
| 权限管理 | `NativeBridge.requestPermission()` | `__onPermissionResult(granted)` |
| 平台检测 | `NativeBridge.getPlatform()` | — |
| 状态查询 | `NativeBridge.isSpeaking()` / `NativeBridge.hasPermission()` | — |

## 编译

### 方式 1：在 Android Studio 中
1. File → Open → 选择本目录
2. 等待 Gradle 同步完成
3. Run → Run 'app'（或者 Build → Build APK）

### 方式 2：命令行（需要 Java + Android SDK）
```bash
chmod +x build.sh
./build.sh
```

### 方式 3：GitHub Actions（在线编译）
详见 `.github/workflows/build-apk.yml`

## 配置自定义服务器地址

默认连接 `https://192.168.3.12:3443`。如果需要修改：

编辑 `app/src/main/java/com/xiaoke/speaker/MainActivity.java` 中的 `SERVER_URL` 常量。
