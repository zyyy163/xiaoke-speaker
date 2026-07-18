#!/bin/bash
# 小科科普助手 APK 编译脚本
# 需要：Java 17+、Android SDK

set -e

echo "=== 设置环境变量 ==="
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}"

echo "=== 生成调试 APK ==="
./gradlew assembleDebug

echo "=== 输出 ==="
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "✅ APK 已生成: $APK_PATH"
    ls -lh "$APK_PATH"
else
    echo "❌ APK 生成失败"
    exit 1
fi
