# AGENTS.md — 给 AI 编码助手的项目说明

## 项目概述

「拼音乐园」：面向 6–7 岁一年级学生的拼音学习应用。Android TV 为主要场景（遥控器 D-pad 焦点导航），同时支持平板横屏触控。课程内容为 2024 统编版语文一年级上册拼音（3 单元 13 课、47 字母、16 整体认读），课程数据在 `app/src/main/assets/lessons.json`。全离线：逐行台词预合成的单声道 MP3 语音包 + 霞鹜文楷子集字体，界面全部为 Jetpack Compose（无 XML 布局）。

## 常用命令

```bash
./gradlew :app:assembleDebug          # 构建 Debug APK
./gradlew :app:testDebugUnitTest      # 52 个单元测试（JUnit4 + coroutines-test，纯 JVM）
./gradlew :app:assembleRelease        # 构建 Release APK：产物 app/build/outputs/apk/release/xuepinyin-v<版本>.apk（R8 混淆；签名读 ~/.gradle/gradle.properties 的 RELEASE_* 属性，缺失时回退 debug 签名并告警）
adb install -r app/build/outputs/apk/debug/xuepinyin-v<版本>-debug.apk
```

- 改动 `domain/`（出题器、学习进度、笔顺拆解）后必须跑测试。
- UI 改动需双端冒烟：TV 模拟器 `szj_tv`（D-pad 焦点导航）+ 手机横屏 AVD（触控），两档字号都不破版。
- Android SDK 在 `D:\develop\androidsdk`（由 `local.properties` 指定）。

## 版本与网络

- 依赖版本有意锁定，勿随手升级：AGP 9.1.0、Kotlin 2.2.21、Compose BOM 2026.09.00、Gradle 9.5.1；compileSdk 37 / minSdk 23 / targetSdk 36 / JVM target 17。升级前先征得确认。
- 为支持 minSdk 23（小米盒子4），navigation-compose 锁 2.9.8、datastore-preferences 锁 1.2.1——它们之后的版本（navigation 2.10+ / datastore 1.3+）声明 minSdk 24。
- Maven 仓库走阿里云镜像（`settings.gradle.kts`），Gradle 发行版走腾讯镜像（`gradle-wrapper.properties`）。境外直连失败时启用 `gradle.properties` 里注释掉的代理配置（127.0.0.1:9999）。

## 目录结构

```
app/src/main/java/com/rakor/xuepinyin/
├── audio/      VoicePackPlayer：MP3 语音播放
├── data/       课程资源（AssetsCourseRepository）、进度与设置（DataStore）
├── domain/     纯 Kotlin 领域逻辑：model / progress / game（出题器）/ stroke（笔顺）——单元测试只覆盖这一层
├── ui/         home / lesson / write / read / play / table / parent / theme
└── MainActivity.kt
app/src/main/assets/   lessons.json（课程数据）、audio/（逐行 MP3）、voicepack/（清单）
tools/                 gen_voice_content.py、subset_font.py 等语音/字体管线脚本（*.ttf 不入库）
docs/                  设计文档与 README 截图（docs/screenshots/）
```

## 主题与 UI 约定（改 UI 前必读）

- 风格「童趣暖阳·奶油圆角」：奶油垂直渐变底（由 `ui/theme/AppTheme.kt` 统一铺底，各屏幕自身透明）、白卡大圆角无描边、珊瑚橙主色、暖棕文字（禁用纯黑）。
- 按钮一律 `import com.rakor.xuepinyin.ui.theme.Button`（`AppButton.kt` 的自定义实现），不要用 material3 的 `Button`——其聚焦底色为灰色且不可定制，与暖色系冲突。
- 焦点态用 `appFocus()` 修饰符（`TvFocus.kt`：放大 + 蜜橙染 + 珊瑚描边）。不要给按钮加投影（黑阴影在奶油底上会糊成脏光晕），不要 clip。
- 拼音教学功能色固定：声母蓝 `2D6CDF`、韵母红 `D9455F`、整体认读绿 `3F8F6B`。
- UI 不使用 emoji（图标用 `res/drawable` 矢量图），文案不出现吉祥物角色。

## 霞鹜文楷字体陷阱

- 文楷行框约 1.184em 且比系统字体宽：定宽容器一律用 `widthIn(min=)`，不要 `width()`。
- 固定高度容器内的多行文本必须显式 `lineHeight`，否则 Text 按自身边界裁剪，拼音下降部（y/g/j/q/p）会被切掉。
- 文楷的 g 为单层字形，是正常字形，不要当缺字处理。

## 字体子集与语音包管线（改课程内容时）

1. 改 `lessons.json` 后重跑 `tools/gen_voice_content.py` 生成 `tools/voice_script.txt`，再走 TTS 合成管线重做 `assets/audio/`；多音字替换表在 `tools/tts_substitutions.json`。
2. 改了任何 `.kt` 或 `lessons.json` 的文案后，重跑 `tools/subset_font.py` 重新子集化字体——脚本扫描全部源码与课程数据，缺字即失败；全量 `.ttf` 在 tools/ 下但已 gitignore。
3. 合成用的 staging 目录不要放在 `build/` 下（gradle clean 会清掉）。
4. assets 音频为单声道 MP3（24kHz/48kbps CBR，与 threecharactorscripture 一致），构建时 `noCompress += "mp3"`（MediaPlayer openFd 要求）；TTS 合成产物用 `ffmpeg -c:a libmp3lame -b:a 48k -ar 24000 -ac 1` 转码并重写各单元 manifest 时长。

## 输入模式与设置

- `InputMode`（FOCUS/TOUCH）区分遥控器与触控：触控端 fontScale ×0.85，布局需在两档字号下都不破版。
- 护眼提醒（20 分钟弹层）逻辑在 `ui/PinyinApp.kt`，开关存于 DataStore（家长中心页）。

## 入库红线

- `local.properties`、`tools/*.ttf` 均已 gitignore，不要提交，也不要改动 `.gitignore` 把它们放进来。
- Release 签名来自 `~/.gradle/gradle.properties` 的 `RELEASE_*` 属性（`D:/develop/key/rakor.jks`，alias=rakor，与 threecharactorscripture 共用同一证书）。仓库内不含任何签名文件；`release.keystore`、`keystore.properties` 的 gitignore 规则保留作兜底。签名属性缺失时 release 构建回退 debug 签名并告警（仅适合自测）。
- UI 大改后同步更新 `docs/screenshots/` 与 README 中的截图。
