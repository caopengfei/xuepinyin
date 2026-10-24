# 拼音乐园 Plan 1/6：项目骨架 + 糖果主题 + 课程数据 + 首页 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立可安装在 Android TV 上的应用骨架：糖果乐园主题、完整课程数据（3 单元 13 课 47 字母）、首页课程地图（D-pad 焦点导航可用）。

**Architecture:** 单 Activity + Navigation Compose；`ui/`（主题与页面）、`domain/model`（课程模型与解析）、`data/`（assets 课程仓库）。课程内容为 `assets/lessons.json`，经 kotlinx.serialization 解析，纯 JVM 可测。其余四模块与课程页本计划先挂占位路由（后续计划逐个替换）。

**Tech Stack:** Kotlin 2.2.21、AGP 9.1.0（AGP 9 内建 Kotlin 支持，无需 kotlin-android 插件）、Gradle 9.5.1、Compose BOM 2026.09.00（material3）、androidx.tv（tv-material 1.1.0 / tv-foundation 1.0.0）、Navigation Compose 2.10.2、kotlinx.serialization 1.9.0、JUnit4。compileSdk 37 / minSdk 24 / targetSdk 36（本机实测可构建；AGP9 与新 androidx 库的最低要求，minSdk 由 21 上调至 24，Android 7.0+ 设备覆盖）。

**设计依据:** `docs/superpowers/specs/2026-10-24-pinyin-tv-app-design.md`

---

## 执行环境注意（Windows + Git Bash，必读）

1. **JDK 17+ 必需**（AGP 8.x 要求；本机已装 GraalVM JDK 25，满足）。执行 `java -version` 确认 ≥17 即可。本机 Gradle CLI 为 9.5.1，与 wrapper 版本一致。
2. **国内镜像优先**：Gradle 分发包用腾讯镜像（见 Task 1 的 `gradle-wrapper.properties`）；Maven 仓库用阿里云镜像（见 `settings.gradle.kts`），国内直连即可，无需代理。
3. **代理兜底**：若镜像缺少某个制品需要回退 `google()`/`mavenCentral()`，取消 `gradle.properties` 中代理四行的注释（本机代理 `127.0.0.1:9999`，见全局说明）。
4. 所有 `gradlew` 命令在仓库根 `D:\develop\src\rakor\xuepinyin` 下用 Git Bash 执行（`./gradlew ...`）。
5. 本地单元测试不依赖 Android 框架（不用 Robolectric），保证 `./gradlew :app:testDebugUnitTest` 快速可跑。

## 文件结构总览（本计划新建）

```
settings.gradle.kts                      # 仓库与镜像配置
build.gradle.kts                         # 根插件声明
gradle.properties                        # JVM 参数 + 代理兜底
gradle/wrapper/gradle-wrapper.properties # 腾讯镜像分发包
app/build.gradle.kts                     # 应用模块
app/src/main/AndroidManifest.xml         # 双启动入口 + 横屏锁定 + leanback
app/src/main/res/values/themes.xml
app/src/main/res/drawable/ic_launcher.xml
app/src/main/res/drawable/tv_banner.xml
app/src/main/java/com/rakor/xuepinyin/MainActivity.kt
app/src/main/java/com/rakor/xuepinyin/ui/theme/CandyTheme.kt
app/src/main/java/com/rakor/xuepinyin/ui/PinyinApp.kt          # 导航壳
app/src/main/java/com/rakor/xuepinyin/ui/StubModuleScreen.kt
app/src/main/java/com/rakor/xuepinyin/domain/model/Course.kt   # 领域模型
app/src/main/java/com/rakor/xuepinyin/data/CourseParser.kt
app/src/main/java/com/rakor/xuepinyin/data/AssetsCourseRepository.kt
app/src/main/java/com/rakor/xuepinyin/domain/ProgressRule.kt   # 下节课推导
app/src/main/java/com/rakor/xuepinyin/ui/home/HomeScreen.kt
app/src/main/assets/lessons.json         # 13 课完整内容
app/src/test/java/com/rakor/xuepinyin/CourseParserTest.kt
app/src/test/java/com/rakor/xuepinyin/CourseIntegrityTest.kt
app/src/test/java/com/rakor/xuepinyin/AssetsCourseRepositoryTest.kt
app/src/test/java/com/rakor/xuepinyin/ProgressRuleTest.kt
```

---

### Task 1: Gradle 脚手架（可构建的空应用）

**Files:**
- Create: `settings.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`app/build.gradle.kts`、`app/src/main/AndroidManifest.xml`（最小版）、`app/.gitignore`

- [ ] **Step 1: 确认 JDK 17 与 Gradle**

Run: `java -version`（期望 ≥17；本机为 25）；`gradle -v`（期望 9.5.1；若无：从 https://mirrors.cloud.tencent.com/gradle/ 下载 9.5.1 zip 解压并加入 PATH）。

- [ ] **Step 2: 写 `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}
rootProject.name = "xuepinyin"
include(":app")
```

- [ ] **Step 3: 写根 `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
}
```

- [ ] **Step 4: 写 `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
# 代理兜底：需要直连 google()/mavenCentral() 时取消注释
# systemProp.http.proxyHost=127.0.0.1
# systemProp.http.proxyPort=9999
# systemProp.https.proxyHost=127.0.0.1
# systemProp.https.proxyPort=9999
```

- [ ] **Step 5: 生成 wrapper 并指向腾讯镜像**

Run: `gradle wrapper --gradle-version 9.5.1`

然后编辑 `gradle/wrapper/gradle-wrapper.properties`，将 `distributionUrl` 一行替换为：

```properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-9.5.1-bin.zip
```

- [ ] **Step 6: 写 `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.rakor.xuepinyin"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rakor.xuepinyin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.tv:tv-foundation:1.0.0")
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 7: 写最小 Manifest 与模块 .gitignore**

`app/src/main/AndroidManifest.xml`（Task 2 会扩成完整版）：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

`app/.gitignore`：

```
/build
```

- [ ] **Step 8: 构建验证**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`（首次会下载分发包与依赖，国内镜像应全程无需代理）。

- [ ] **Step 9: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/ gradlew gradlew.bat app/
git commit -m "build: Android 应用脚手架（AGP 8.7.3 + Compose + 国内镜像）"
```

---

### Task 2: Manifest 双入口 + MainActivity + 糖果主题

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/themes.xml`、`app/src/main/res/drawable/ic_launcher.xml`、`app/src/main/res/drawable/tv_banner.xml`、`app/src/main/java/com/rakor/xuepinyin/MainActivity.kt`、`app/src/main/java/com/rakor/xuepinyin/ui/theme/CandyTheme.kt`

- [ ] **Step 1: 写完整 Manifest（双启动入口、锁定横屏、leanback）**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature
        android:name="android.software.leanback"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />

    <application
        android:label="拼音乐园"
        android:icon="@drawable/ic_launcher"
        android:banner="@drawable/tv_banner"
        android:theme="@style/Theme.XuePinyin"
        android:allowBackup="true"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="sensorLandscape"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 2: 写主题与图标资源**

`app/src/main/res/values/themes.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.XuePinyin" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">#FFF6E9</item>
    </style>
</resources>
```

`app/src/main/res/drawable/ic_launcher.xml`（矢量糖纸圆标，API 21+ 可用）：

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp" android:height="48dp"
    android:viewportWidth="48" android:viewportHeight="48">
    <path android:fillColor="#FF6B6B" android:pathData="M24,4 A20,20 0 1,1 23.9,4 Z" />
    <path android:fillColor="#FFC94D" android:pathData="M24,10 A14,14 0 1,1 23.9,10 Z" />
    <path android:fillColor="#FFF6E9" android:pathData="M14,22 h20 v6 a6,6 0 0,1 -6,6 h-8 a6,6 0 0,1 -6,-6 Z" />
</vector>
```

`app/src/main/res/drawable/tv_banner.xml`（TV 横幅，320×180 比例）：

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="320dp" android:height="180dp"
    android:viewportWidth="320" android:viewportHeight="180">
    <path android:fillColor="#FFF6E9" android:pathData="M0,0 h320 v180 h-320 Z" />
    <path android:fillColor="#FF6B6B" android:pathData="M20,60 a30,30 0 1,1 0.1,0 Z" />
    <path android:fillColor="#43C8B0" android:pathData="M250,30 a22,22 0 1,1 0.1,0 Z" />
    <path android:fillColor="#FFC94D" android:pathData="M120,110 a26,26 0 1,1 0.1,0 Z" />
</vector>
```

- [ ] **Step 3: 写 `ui/theme/CandyTheme.kt`（设计 Token + 焦点修饰符）**

```kotlin
package com.rakor.xuepinyin.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CandyColors {
    val Background = Color(0xFFFFF6E9)
    val Ink = Color(0xFF4A3B32)
    val InkSoft = Color(0xFF8A7A6D)
    val Coral = Color(0xFFFF6B6B)
    val Sun = Color(0xFFFFC94D)
    val Mint = Color(0xFF43C8B0)
    val Sky = Color(0xFF54A0FF)
    val Grape = Color(0xFF9B7EDE)
    val Card = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFECDCC8)
    val FinalRed = Color(0xFFD9455F)
    val InitialBlue = Color(0xFF2D6CDF)
    val HolisticGreen = Color(0xFF3F8F6B)
}

object CandyType {
    val caption = 16.sp
    val body = 20.sp
    val title = 28.sp
    val heroTitle = 56.sp
    val letter = 30.sp
    val bigLetter = 120.sp
}

private val CandyScheme = lightColorScheme(
    primary = CandyColors.Coral,
    onPrimary = Color.White,
    background = CandyColors.Background,
    onBackground = CandyColors.Ink,
    surface = CandyColors.Card,
    onSurface = CandyColors.Ink,
    secondary = CandyColors.Sun,
    onSecondary = CandyColors.Ink,
)

@Composable
fun CandyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CandyScheme, content = content)
}

/** TV 焦点态：放大 1.06 + 深可可描边 + 硬阴影（平板触控下不触发，视觉自动弱化） */
fun Modifier.candyFocus(shape: RoundedCornerShape = RoundedCornerShape(12.dp)): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .graphicsLayer {
            val scale = if (focused) 1.06f else 1f
            scaleX = scale
            scaleY = scale
        }
        .border(
            width = if (focused) 3.dp else 0.dp,
            color = if (focused) CandyColors.Ink else Color.Transparent,
            shape = shape,
        )
        .shadow(elevation = if (focused) 8.dp else 0.dp, shape = shape)
}
```

- [ ] **Step 4: 写 `MainActivity.kt`（临时直接显示主题色背景，Task 6 换成导航壳）**

```kotlin
package com.rakor.xuepinyin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.CandyTheme
import com.rakor.xuepinyin.ui.theme.CandyType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CandyTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(CandyColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("拼音乐园", fontSize = CandyType.heroTitle, color = CandyColors.Ink)
                }
            }
        }
    }
}
```

- [ ] **Step 5: 构建 + 安装冒烟**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。
Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`（需已启动任一 TV 模拟器或连接设备；`adb devices` 确认）。
Expected: `Success`。启动后整屏奶油底、居中「拼音乐园」。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/ app/src/main/java/
git commit -m "feat: 双入口 Manifest（TV/平板横屏）与糖果乐园主题 Token"
```

---

### Task 3: 课程领域模型 + JSON 解析器（TDD）

**Files:**
- Create: `app/src/main/java/com/rakor/xuepinyin/domain/model/Course.kt`
- Create: `app/src/main/java/com/rakor/xuepinyin/data/CourseParser.kt`
- Test: `app/src/test/java/com/rakor/xuepinyin/CourseParserTest.kt`

- [ ] **Step 1: 写失败测试 `CourseParserTest.kt`**

```kotlin
package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.CourseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseParserTest {

    private val sample = """
    {
      "units": [
        {"id": 1, "title": "拼音一", "lessonStart": 1, "lessonEnd": 2}
      ],
      "lessons": [
        {
          "id": 1, "unitId": 1, "title": "a o e", "note": null,
          "letters": [
            {"pinyin": "a", "type": "SINGLE_FINAL", "tones": ["ā","á","ǎ","à"],
             "mnemonic": "张大嘴巴 a a a", "sceneEmoji": "👧", "sceneWord": "啊"},
            {"pinyin": "b", "type": "INITIAL", "tones": [],
             "mnemonic": "听广播 b b b", "sceneEmoji": "📻", "sceneWord": null}
          ],
          "toneDrills": ["ā á ǎ à"],
          "syllables": [],
          "holisticSyllables": []
        }
      ]
    }
    """

    @Test
    fun `解析单元与课程基础字段`() {
        val course = CourseParser.parse(sample)
        assertEquals(1, course.units.size)
        assertEquals("拼音一", course.units[0].title)
        assertEquals(1..2, course.units[0].lessonRange)
        assertEquals(1, course.lessons.size)
        assertEquals("a o e", course.lessons[0].title)
    }

    @Test
    fun `解析字母类型与四声`() {
        val a = CourseParser.parse(sample).lessons[0].letters[0]
        assertEquals("SINGLE_FINAL", a.type.name)
        assertEquals(listOf("ā", "á", "ǎ", "à"), a.tones)
        assertEquals("👧", a.sceneEmoji)
    }

    @Test
    fun `声母无声调与可空字段`() {
        val b = CourseParser.parse(sample).lessons[0].letters[1]
        assertEquals(0, b.tones.size)
        assertEquals(null, b.sceneWord)
    }

    @Test
    fun `coursesOf 按 unitId 过滤`() {
        val course = CourseParser.parse(sample)
        assertEquals(1, course.lessonsOf(1).size)
        assertTrue(course.lessonsOf(9).isEmpty())
    }

    @Test(expected = CourseParser.ParseException::class)
    fun `非法 JSON 抛出 ParseException`() {
        CourseParser.parse("{ 不是 json")
    }
}
```

- [ ] **Step 2: 运行验证失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rakor.xuepinyin.CourseParserTest"`
Expected: 编译失败（`CourseParser` / `Course` 未定义）。

- [ ] **Step 3: 写模型 `domain/model/Course.kt`**

```kotlin
package com.rakor.xuepinyin.domain.model

/** 字母类型：单韵母 / 复韵母（含前后鼻韵母）/ 声母 / 特殊韵母(er) */
enum class LetterType { SINGLE_FINAL, COMPOUND_FINAL, INITIAL, SPECIAL }

data class PinyinLetter(
    val pinyin: String,
    val type: LetterType,
    val tones: List<String>,
    val mnemonic: String?,
    val sceneEmoji: String?,
    val sceneWord: String?,
)

data class Syllable(
    val initial: String,
    val finalPinyin: String,
    val result: String,
    val word: String?,
)

data class Lesson(
    val id: Int,
    val unitId: Int,
    val title: String,
    val note: String?,
    val letters: List<PinyinLetter>,
    val toneDrills: List<String>,
    val syllables: List<Syllable>,
    val holisticSyllables: List<String>,
)

data class UnitGroup(
    val id: Int,
    val title: String,
    val lessonRange: IntRange,
)

data class Course(
    val units: List<UnitGroup>,
    val lessons: List<Lesson>,
) {
    fun lessonsOf(unitId: Int): List<Lesson> = lessons.filter { it.unitId == unitId }
    fun lesson(id: Int): Lesson? = lessons.firstOrNull { it.id == id }
    fun allLetters(): List<PinyinLetter> = lessons.flatMap { it.letters }
}
```

- [ ] **Step 4: 写解析器 `data/CourseParser.kt`**

```kotlin
package com.rakor.xuepinyin.data

import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.model.Lesson
import com.rakor.xuepinyin.domain.model.LetterType
import com.rakor.xuepinyin.domain.model.PinyinLetter
import com.rakor.xuepinyin.domain.model.Syllable
import com.rakor.xuepinyin.domain.model.UnitGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object CourseParser {

    class ParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

    @Serializable
    private data class UnitDto(val id: Int, val title: String, val lessonStart: Int, val lessonEnd: Int)

    @Serializable
    private data class LetterDto(
        val pinyin: String,
        val type: String,
        val tones: List<String> = emptyList(),
        val mnemonic: String? = null,
        val sceneEmoji: String? = null,
        val sceneWord: String? = null,
    )

    @Serializable
    private data class SyllableDto(
        val initial: String,
        val finalPinyin: String,
        val result: String,
        val word: String? = null,
    )

    @Serializable
    private data class LessonDto(
        val id: Int,
        val unitId: Int,
        val title: String,
        val note: String? = null,
        val letters: List<LetterDto>,
        val toneDrills: List<String> = emptyList(),
        val syllables: List<SyllableDto> = emptyList(),
        val holisticSyllables: List<String> = emptyList(),
    )

    @Serializable
    private data class CourseDto(val units: List<UnitDto>, val lessons: List<LessonDto>)

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): Course = try {
        val dto = json.decodeFromString<CourseDto>(text)
        Course(
            units = dto.units.map { UnitGroup(it.id, it.title, it.lessonStart..it.lessonEnd) },
            lessons = dto.lessons.map { l ->
                Lesson(
                    id = l.id,
                    unitId = l.unitId,
                    title = l.title,
                    note = l.note,
                    letters = l.letters.map { lt ->
                        PinyinLetter(
                            pinyin = lt.pinyin,
                            type = LetterType.valueOf(lt.type),
                            tones = lt.tones,
                            mnemonic = lt.mnemonic,
                            sceneEmoji = lt.sceneEmoji,
                            sceneWord = lt.sceneWord,
                        )
                    },
                    toneDrills = l.toneDrills,
                    syllables = l.syllables.map { s ->
                        Syllable(s.initial, s.finalPinyin, s.result, s.word)
                    },
                    holisticSyllables = l.holisticSyllables,
                )
            },
        )
    } catch (e: IllegalArgumentException) {
        throw ParseException("课程数据格式错误（type 枚举不合法）", e)
    } catch (e: kotlinx.serialization.SerializationException) {
        throw ParseException("课程数据 JSON 解析失败", e)
    }
}
```

- [ ] **Step 5: 运行验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rakor.xuepinyin.CourseParserTest"`
Expected: 5 个测试全部 PASS。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/rakor/xuepinyin/domain/ app/src/main/java/com/rakor/xuepinyin/data/CourseParser.kt app/src/test/
git commit -m "feat: 课程领域模型与 JSON 解析器（TDD）"
```

---

### Task 4: 完整课程数据 lessons.json + 完整性测试（TDD）

**Files:**
- Create: `app/src/main/assets/lessons.json`
- Test: `app/src/test/java/com/rakor/xuepinyin/CourseIntegrityTest.kt`

- [ ] **Step 1: 先写失败测试 `CourseIntegrityTest.kt`**

```kotlin
package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.CourseParser
import com.rakor.xuepinyin.domain.model.LetterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 直接读 assets 源文件，纯 JVM 可跑 */
class CourseIntegrityTest {
    private val course = CourseParser.parse(
        File("src/main/assets/lessons.json").readText(Charsets.UTF_8)
    )

    @Test fun `每课 unitId 与所属单元区间一致`() {
        course.lessons.forEach { l ->
            val unit = course.units.firstOrNull { l.id in it.lessonRange }
            assertTrue("课 ${l.id} 不落在任何单元区间", unit != null)
            assertEquals("课 ${l.id} 的 unitId 错误", unit!!.id, l.unitId)
        }
    }

    @Test fun `3 个单元 13 课，课程号 1-13 连续`() {
        assertEquals(3, course.units.size)
        assertEquals(13, course.lessons.size)
        assertEquals((1..13).toList(), course.lessons.map { it.id })
    }

    @Test fun `单元划分 1-6、7-10、11-13`() {
        assertEquals(1..6, course.units[0].lessonRange)
        assertEquals(7..10, course.units[1].lessonRange)
        assertEquals(11..13, course.units[2].lessonRange)
    }

    @Test fun `47 个字母：单韵母6 复韵母17 特殊1 声母23`() {
        val letters = course.allLetters()
        assertEquals(47, letters.size)
        assertEquals(6, letters.count { it.type == LetterType.SINGLE_FINAL })
        assertEquals(17, letters.count { it.type == LetterType.COMPOUND_FINAL })
        assertEquals(1, letters.count { it.type == LetterType.SPECIAL })
        assertEquals(23, letters.count { it.type == LetterType.INITIAL })
    }

    @Test fun `拼音2 为 i u ü y w`() {
        assertEquals(listOf("i", "u", "ü", "y", "w"), course.lesson(2)!!.letters.map { it.pinyin })
    }

    @Test fun `韵母都有四声且声母无声调`() {
        course.allLetters().forEach { l ->
            when (l.type) {
                LetterType.INITIAL -> assertEquals("声母 ${l.pinyin} 不应有声调", 0, l.tones.size)
                else -> assertEquals("韵母 ${l.pinyin} 应有四声", 4, l.tones.size)
            }
        }
    }

    @Test fun `标调位置抽查：ai 标在 a，ui 标在 i，iu 标在 u`() {
        val letters = course.allLetters()
        assertEquals(listOf("āi", "ái", "ǎi", "ài"), letters.first { it.pinyin == "ai" }.tones)
        assertEquals(listOf("uī", "uí", "uǐ", "uì"), letters.first { it.pinyin == "ui" }.tones)
        assertEquals(listOf("iū", "iú", "iǔ", "iù"), letters.first { it.pinyin == "iu" }.tones)
    }

    @Test fun `整体认读音节共 16 个且分布正确`() {
        val holistic = course.lessons.flatMap { it.holisticSyllables }
        assertEquals(
            setOf("yi", "wu", "yu", "zi", "ci", "si", "zhi", "chi", "shi", "ri",
                  "ye", "yue", "yuan", "yin", "yun", "ying"),
            holistic.toSet(),
        )
        assertEquals(16, holistic.size)
    }

    @Test fun `每课至少一条声调操练`() {
        course.lessons.forEach { l ->
            assertTrue("课 ${l.id} 缺少 toneDrills", l.toneDrills.isNotEmpty())
        }
    }

    @Test fun `每课字母都有情境口诀`() {
        course.allLetters().forEach { l ->
            assertTrue("字母 ${l.pinyin} 缺少口诀", !l.mnemonic.isNullOrBlank())
            assertTrue("字母 ${l.pinyin} 缺少情境图", !l.sceneEmoji.isNullOrBlank())
        }
    }
}
```

- [ ] **Step 2: 运行验证失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rakor.xuepinyin.CourseIntegrityTest"`
Expected: 编译错误或运行失败（`lessons.json` 不存在）。

- [ ] **Step 3: 写完整 `app/src/main/assets/lessons.json`**

```json
{
  "units": [
    {"id": 1, "title": "拼音一 · 单韵母和声母", "lessonStart": 1, "lessonEnd": 6},
    {"id": 2, "title": "拼音二 · 平翘舌和复韵母", "lessonStart": 7, "lessonEnd": 10},
    {"id": 3, "title": "拼音三 · 前后鼻韵母", "lessonStart": 11, "lessonEnd": 13}
  ],
  "lessons": [
    {
      "id": 1, "unitId": 1, "title": "a o e", "note": null,
      "letters": [
        {"pinyin": "a", "type": "SINGLE_FINAL", "tones": ["ā","á","ǎ","à"], "mnemonic": "张大嘴巴 a a a", "sceneEmoji": "👧", "sceneWord": "啊"},
        {"pinyin": "o", "type": "SINGLE_FINAL", "tones": ["ō","ó","ǒ","ò"], "mnemonic": "圆圆嘴巴 o o o", "sceneEmoji": "🐓", "sceneWord": "喔"},
        {"pinyin": "e", "type": "SINGLE_FINAL", "tones": ["ē","é","ě","è"], "mnemonic": "扁扁嘴巴 e e e", "sceneEmoji": "🦢", "sceneWord": "鹅"}
      ],
      "toneDrills": ["ā á ǎ à", "ō ó ǒ ò", "ē é ě è"],
      "syllables": [],
      "holisticSyllables": []
    },
    {
      "id": 2, "unitId": 1, "title": "i u ü y w", "note": "y、w 是声母，帮 i、u、ü 写音节：yi、wu、yu",
      "letters": [
        {"pinyin": "i", "type": "SINGLE_FINAL", "tones": ["ī","í","ǐ","ì"], "mnemonic": "穿新衣 i i i", "sceneEmoji": "👕", "sceneWord": "衣"},
        {"pinyin": "u", "type": "SINGLE_FINAL", "tones": ["ū","ú","ǔ","ù"], "mnemonic": "乌鸦叫 u u u", "sceneEmoji": "🐦", "sceneWord": "乌"},
        {"pinyin": "ü", "type": "SINGLE_FINAL", "tones": ["ǖ","ǘ","ǚ","ǜ"], "mnemonic": "小鱼吐泡 ü ü ü", "sceneEmoji": "🐟", "sceneWord": "鱼"},
        {"pinyin": "y", "type": "INITIAL", "tones": [], "mnemonic": "树杈 y y y", "sceneEmoji": "🌲", "sceneWord": null},
        {"pinyin": "w", "type": "INITIAL", "tones": [], "mnemonic": "屋顶 w w w", "sceneEmoji": "🏠", "sceneWord": null}
      ],
      "toneDrills": ["yī yí yǐ yì", "wū wú wǔ wù", "yú yǔ"],
      "syllables": [],
      "holisticSyllables": ["yi", "wu", "yu"]
    },
    {
      "id": 3, "unitId": 1, "title": "b p m f", "note": null,
      "letters": [
        {"pinyin": "b", "type": "INITIAL", "tones": [], "mnemonic": "听广播 b b b", "sceneEmoji": "📻", "sceneWord": null},
        {"pinyin": "p", "type": "INITIAL", "tones": [], "mnemonic": "爬山坡 p p p", "sceneEmoji": "⛰️", "sceneWord": null},
        {"pinyin": "m", "type": "INITIAL", "tones": [], "mnemonic": "两个门洞 m m m", "sceneEmoji": "🚪", "sceneWord": null},
        {"pinyin": "f", "type": "INITIAL", "tones": [], "mnemonic": "一根拐杖 f f f", "sceneEmoji": "🦯", "sceneWord": null}
      ],
      "toneDrills": ["bā bá bǎ bà", "pō pó pǒ pò", "mā má mǎ mà", "fū fú fǔ fù"],
      "syllables": [
        {"initial": "b", "finalPinyin": "a", "result": "ba", "word": "爸"},
        {"initial": "p", "finalPinyin": "o", "result": "po", "word": "婆"},
        {"initial": "m", "finalPinyin": "i", "result": "mi", "word": "米"},
        {"initial": "f", "finalPinyin": "u", "result": "fu", "word": "父"}
      ],
      "holisticSyllables": []
    },
    {
      "id": 4, "unitId": 1, "title": "d t n l", "note": null,
      "letters": [
        {"pinyin": "d", "type": "INITIAL", "tones": [], "mnemonic": "马蹄声响 d d d", "sceneEmoji": "🐴", "sceneWord": null},
        {"pinyin": "t", "type": "INITIAL", "tones": [], "mnemonic": "一把雨伞 t t t", "sceneEmoji": "☂️", "sceneWord": null},
        {"pinyin": "n", "type": "INITIAL", "tones": [], "mnemonic": "一个门洞 n n n", "sceneEmoji": "🚪", "sceneWord": null},
        {"pinyin": "l", "type": "INITIAL", "tones": [], "mnemonic": "一根小棒 l l l", "sceneEmoji": "🥢", "sceneWord": null}
      ],
      "toneDrills": ["dā dá dǎ dà", "tú tǔ tù", "nǐ nì", "lù lǔ"],
      "syllables": [
        {"initial": "d", "finalPinyin": "a", "result": "da", "word": "大"},
        {"initial": "t", "finalPinyin": "u", "result": "tu", "word": "兔"},
        {"initial": "n", "finalPinyin": "i", "result": "ni", "word": "你"},
        {"initial": "l", "finalPinyin": "ü", "result": "lü", "word": "驴"}
      ],
      "holisticSyllables": []
    },
    {
      "id": 5, "unitId": 1, "title": "g k h", "note": null,
      "letters": [
        {"pinyin": "g", "type": "INITIAL", "tones": [], "mnemonic": "鸽子衔环 g g g", "sceneEmoji": "🕊️", "sceneWord": null},
        {"pinyin": "k", "type": "INITIAL", "tones": [], "mnemonic": "蝌蚪游水 k k k", "sceneEmoji": "🐛", "sceneWord": null},
        {"pinyin": "h", "type": "INITIAL", "tones": [], "mnemonic": "一把椅子 h h h", "sceneEmoji": "🪑", "sceneWord": null}
      ],
      "toneDrills": ["gē gé gě gè", "kě kè", "hú hǔ hù"],
      "syllables": [
        {"initial": "g", "finalPinyin": "e", "result": "ge", "word": "哥"},
        {"initial": "k", "finalPinyin": "e", "result": "ke", "word": "课"},
        {"initial": "h", "finalPinyin": "u", "result": "hu", "word": "虎"}
      ],
      "holisticSyllables": []
    },
    {
      "id": 6, "unitId": 1, "title": "j q x", "note": "小 ü 见到 j、q、x，去掉两点还读 ü",
      "letters": [
        {"pinyin": "j", "type": "INITIAL", "tones": [], "mnemonic": "大母鸡 j j j", "sceneEmoji": "🐔", "sceneWord": null},
        {"pinyin": "q", "type": "INITIAL", "tones": [], "mnemonic": "气球上天 q q q", "sceneEmoji": "🎈", "sceneWord": null},
        {"pinyin": "x", "type": "INITIAL", "tones": [], "mnemonic": "切开西瓜 x x x", "sceneEmoji": "🍉", "sceneWord": null}
      ],
      "toneDrills": ["jī jí jǐ jì", "qī qí qǐ qì", "xī xí xǐ xì"],
      "syllables": [
        {"initial": "j", "finalPinyin": "i", "result": "ji", "word": "鸡"},
        {"initial": "q", "finalPinyin": "i", "result": "qi", "word": "棋"},
        {"initial": "x", "finalPinyin": "i", "result": "xi", "word": "西"},
        {"initial": "j", "finalPinyin": "ü", "result": "ju", "word": "菊"},
        {"initial": "q", "finalPinyin": "ü", "result": "qu", "word": "区"},
        {"initial": "x", "finalPinyin": "ü", "result": "xu", "word": "须"}
      ],
      "holisticSyllables": []
    },
    {
      "id": 7, "unitId": 2, "title": "z c s", "note": null,
      "letters": [
        {"pinyin": "z", "type": "INITIAL", "tones": [], "mnemonic": "像个 2 字 z z z", "sceneEmoji": "2️⃣", "sceneWord": null},
        {"pinyin": "c", "type": "INITIAL", "tones": [], "mnemonic": "半个圆圈 c c c", "sceneEmoji": "🌗", "sceneWord": null},
        {"pinyin": "s", "type": "INITIAL", "tones": [], "mnemonic": "蚕儿吐丝 s s s", "sceneEmoji": "🧵", "sceneWord": null}
      ],
      "toneDrills": ["zī zí zǐ zì", "cī cí cǐ cì", "sī sí sì"],
      "syllables": [
        {"initial": "z", "finalPinyin": "i", "result": "zi", "word": "字"},
        {"initial": "c", "finalPinyin": "i", "result": "ci", "word": "刺"},
        {"initial": "s", "finalPinyin": "i", "result": "si", "word": "丝"}
      ],
      "holisticSyllables": ["zi", "ci", "si"]
    },
    {
      "id": 8, "unitId": 2, "title": "zh ch sh r", "note": "zh、ch、sh、r 是翘舌音，舌头要翘起来",
      "letters": [
        {"pinyin": "zh", "type": "INITIAL", "tones": [], "mnemonic": "织毛衣 zh zh zh", "sceneEmoji": "🧶", "sceneWord": null},
        {"pinyin": "ch", "type": "INITIAL", "tones": [], "mnemonic": "吃苹果 ch ch ch", "sceneEmoji": "🍎", "sceneWord": null},
        {"pinyin": "sh", "type": "INITIAL", "tones": [], "mnemonic": "威风狮子 sh sh sh", "sceneEmoji": "🦁", "sceneWord": null},
        {"pinyin": "r", "type": "INITIAL", "tones": [], "mnemonic": "一轮红日 r r r", "sceneEmoji": "☀️", "sceneWord": null}
      ],
      "toneDrills": ["zhī zhí zhǐ zhì", "chī chí chǐ chì", "shī shí shǐ shì", "rì"],
      "syllables": [
        {"initial": "zh", "finalPinyin": "i", "result": "zhi", "word": "纸"},
        {"initial": "ch", "finalPinyin": "e", "result": "che", "word": "车"},
        {"initial": "sh", "finalPinyin": "u", "result": "shu", "word": "书"},
        {"initial": "r", "finalPinyin": "i", "result": "ri", "word": "日"}
      ],
      "holisticSyllables": ["zhi", "chi", "shi", "ri"]
    },
    {
      "id": 9, "unitId": 2, "title": "ai ei ui", "note": null,
      "letters": [
        {"pinyin": "ai", "type": "COMPOUND_FINAL", "tones": ["āi","ái","ǎi","ài"], "mnemonic": "挨在一起 ai ai ai", "sceneEmoji": "👫", "sceneWord": "挨"},
        {"pinyin": "ei", "type": "COMPOUND_FINAL", "tones": ["ēi","éi","ěi","èi"], "mnemonic": "用力砍柴 ei ei ei", "sceneEmoji": "🪓", "sceneWord": "欸"},
        {"pinyin": "ui", "type": "COMPOUND_FINAL", "tones": ["uī","uí","uǐ","uì"], "mnemonic": "围上围巾 ui ui ui", "sceneEmoji": "🧣", "sceneWord": "围"}
      ],
      "toneDrills": ["āi ái ǎi ài", "ēi éi ěi èi", "uī uí uǐ uì"],
      "syllables": [
        {"initial": "b", "finalPinyin": "ai", "result": "bai", "word": "白"},
        {"initial": "h", "finalPinyin": "ei", "result": "hei", "word": "黑"},
        {"initial": "d", "finalPinyin": "ui", "result": "dui", "word": "队"}
      ],
      "holisticSyllables": []
    },
    {
      "id": 10, "unitId": 2, "title": "ao ou iu", "note": null,
      "letters": [
        {"pinyin": "ao", "type": "COMPOUND_FINAL", "tones": ["āo","áo","ǎo","ào"], "mnemonic": "穿上棉袄 ao ao ao", "sceneEmoji": "🧥", "sceneWord": "袄"},
        {"pinyin": "ou", "type": "COMPOUND_FINAL", "tones": ["ōu","óu","ǒu","òu"], "mnemonic": "水里莲藕 ou ou ou", "sceneEmoji": "🌊", "sceneWord": "藕"},
        {"pinyin": "iu", "type": "COMPOUND_FINAL", "tones": ["iū","iú","iǔ","iù"], "mnemonic": "邮递员 iu iu iu", "sceneEmoji": "📮", "sceneWord": "邮"}
      ],
      "toneDrills": ["āo áo ǎo ào", "ōu óu ǒu òu", "iū iú iǔ iù"],
      "syllables": [
        {"initial": "m", "finalPinyin": "ao", "result": "mao", "word": "猫"},
        {"initial": "l", "finalPinyin": "ou", "result": "lou", "word": "楼"},
        {"initial": "n", "finalPinyin": "iu", "result": "niu", "word": "牛"}
      ],
      "holisticSyllables": []
    },
    {
      "id": 11, "unitId": 3, "title": "ie üe er", "note": "er 是特殊韵母，自己就能成音节",
      "letters": [
        {"pinyin": "ie", "type": "COMPOUND_FINAL", "tones": ["iē","ié","iě","iè"], "mnemonic": "椰树高高 ie ie ie", "sceneEmoji": "🌴", "sceneWord": "椰"},
        {"pinyin": "üe", "type": "COMPOUND_FINAL", "tones": ["üē","üé","üě","üè"], "mnemonic": "一轮弯月 üe üe üe", "sceneEmoji": "🌙", "sceneWord": "月"},
        {"pinyin": "er", "type": "SPECIAL", "tones": ["ēr","ér","ěr","èr"], "mnemonic": "竖起耳朵 er er er", "sceneEmoji": "👂", "sceneWord": "耳"}
      ],
      "toneDrills": ["iē ié iě iè", "üē üé üě üè", "ēr ér ěr èr"],
      "syllables": [
        {"initial": "x", "finalPinyin": "ie", "result": "xie", "word": "写"},
        {"initial": "n", "finalPinyin": "üe", "result": "nüe", "word": "虐"},
        {"initial": "l", "finalPinyin": "üe", "result": "lüe", "word": "略"}
      ],
      "holisticSyllables": ["ye", "yue"]
    },
    {
      "id": 12, "unitId": 3, "title": "an en in un ün", "note": null,
      "letters": [
        {"pinyin": "an", "type": "COMPOUND_FINAL", "tones": ["ān","án","ǎn","àn"], "mnemonic": "天安门 an an an", "sceneEmoji": "🏛️", "sceneWord": "安"},
        {"pinyin": "en", "type": "COMPOUND_FINAL", "tones": ["ēn","én","ěn","èn"], "mnemonic": "摁摁门铃 en en en", "sceneEmoji": "🔔", "sceneWord": "摁"},
        {"pinyin": "in", "type": "COMPOUND_FINAL", "tones": ["īn","ín","ǐn","ìn"], "mnemonic": "树荫下面 in in in", "sceneEmoji": "🌳", "sceneWord": "荫"},
        {"pinyin": "un", "type": "COMPOUND_FINAL", "tones": ["ūn","ún","ǔn","ùn"], "mnemonic": "蚊子嗡嗡 un un un", "sceneEmoji": "🦟", "sceneWord": "蚊"},
        {"pinyin": "ün", "type": "COMPOUND_FINAL", "tones": ["ǖn","ǘn","ǚn","ǜn"], "mnemonic": "朵朵白云 ün ün ün", "sceneEmoji": "☁️", "sceneWord": "云"}
      ],
      "toneDrills": ["ān án ǎn àn", "ēn én ěn èn", "īn ín ǐn ìn", "ūn ún ǔn ùn", "ǖn ǘn ǚn ǜn"],
      "syllables": [
        {"initial": "b", "finalPinyin": "an", "result": "ban", "word": "班"},
        {"initial": "ch", "finalPinyin": "en", "result": "chen", "word": "陈"},
        {"initial": "h", "finalPinyin": "un", "result": "hun", "word": "混"}
      ],
      "holisticSyllables": ["yuan", "yin", "yun"]
    },
    {
      "id": 13, "unitId": 3, "title": "ang eng ing ong", "note": null,
      "letters": [
        {"pinyin": "ang", "type": "COMPOUND_FINAL", "tones": ["āng","áng","ǎng","àng"], "mnemonic": "小羊上山 ang ang ang", "sceneEmoji": "🐐", "sceneWord": "羊"},
        {"pinyin": "eng", "type": "COMPOUND_FINAL", "tones": ["ēng","éng","ěng","èng"], "mnemonic": "台灯发亮 eng eng eng", "sceneEmoji": "💡", "sceneWord": "灯"},
        {"pinyin": "ing", "type": "COMPOUND_FINAL", "tones": ["īng","íng","ǐng","ìng"], "mnemonic": "老鹰飞翔 ing ing ing", "sceneEmoji": "🦅", "sceneWord": "鹰"},
        {"pinyin": "ong", "type": "COMPOUND_FINAL", "tones": ["ōng","óng","ǒng","òng"], "mnemonic": "钟声嗡嗡 ong ong ong", "sceneEmoji": "⏰", "sceneWord": "钟"}
      ],
      "toneDrills": ["āng áng ǎng àng", "ēng éng ěng èng", "īng íng ǐng ìng", "ōng óng ǒng òng"],
      "syllables": [
        {"initial": "sh", "finalPinyin": "an", "result": "shan", "word": "山"},
        {"initial": "d", "finalPinyin": "eng", "result": "deng", "word": "灯"},
        {"initial": "m", "finalPinyin": "ing", "result": "ming", "word": "明"},
        {"initial": "zh", "finalPinyin": "ong", "result": "zhong", "word": "中"}
      ],
      "holisticSyllables": ["ying"]
    }
  ]
}
```

- [ ] **Step 4: 运行验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rakor.xuepinyin.CourseIntegrityTest"`
Expected: 9 个测试全部 PASS（若失败按断言修 JSON，不许改断言）。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/lessons.json app/src/test/java/com/rakor/xuepinyin/CourseIntegrityTest.kt
git commit -m "feat: 13 课完整课程数据与完整性校验（统编版一上拼音）"
```

---

### Task 5: AssetsCourseRepository（TDD）

**Files:**
- Create: `app/src/main/java/com/rakor/xuepinyin/data/AssetsCourseRepository.kt`
- Test: `app/src/test/java/com/rakor/xuepinyin/AssetsCourseRepositoryTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.AssetsCourseRepository
import com.rakor.xuepinyin.data.CourseParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AssetsCourseRepositoryTest {

    private fun repoOf(path: String) = AssetsCourseRepository { File(path).readText(Charsets.UTF_8) }

    @Test
    fun `加载真实 assets 并解析为 13 课`() {
        val repo = repoOf("src/main/assets/lessons.json")
        assertEquals(13, repo.course.lessons.size)
        assertEquals("a o e", repo.course.lesson(1)!!.title)
    }

    @Test
    fun `course 惰性加载且缓存同一实例`() {
        var reads = 0
        val repo = AssetsCourseRepository {
            reads++
            File("src/main/assets/lessons.json").readText(Charsets.UTF_8)
        }
        repo.course
        repo.course
        assertEquals(1, reads)
    }

    @Test(expected = CourseParser.ParseException::class)
    fun `坏数据抛 ParseException 而非崩溃为其他类型`() {
        AssetsCourseRepository { "{ bad" }.course
    }
}
```

- [ ] **Step 2: 运行验证失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rakor.xuepinyin.AssetsCourseRepositoryTest"`
Expected: 编译失败（类不存在）。

- [ ] **Step 3: 写实现**

```kotlin
package com.rakor.xuepinyin.data

import android.content.Context
import com.rakor.xuepinyin.domain.model.Course

/** 课程数据仓库：生产环境从 assets 读取，测试注入任意 JSON 提供者 */
class AssetsCourseRepository(private val jsonProvider: () -> String) {

    val course: Course by lazy { CourseParser.parse(jsonProvider()) }

    companion object {
        fun fromAssets(context: Context): AssetsCourseRepository =
            AssetsCourseRepository {
                context.assets.open("lessons.json").bufferedReader().use { it.readText() }
            }
    }
}
```

- [ ] **Step 4: 运行验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rakor.xuepinyin.AssetsCourseRepositoryTest"`
Expected: 3 个测试 PASS。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/rakor/xuepinyin/data/AssetsCourseRepository.kt app/src/test/java/com/rakor/xuepinyin/AssetsCourseRepositoryTest.kt
git commit -m "feat: 课程数据仓库（assets 加载 + 可注入测试）"
```

---

### Task 6: 下节课推导规则（TDD）+ 首页 UI + 导航壳

**Files:**
- Create: `app/src/main/java/com/rakor/xuepinyin/domain/ProgressRule.kt`
- Create: `app/src/main/java/com/rakor/xuepinyin/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/rakor/xuepinyin/ui/PinyinApp.kt`
- Create: `app/src/main/java/com/rakor/xuepinyin/ui/StubModuleScreen.kt`
- Modify: `app/src/main/java/com/rakor/xuepinyin/MainActivity.kt`
- Test: `app/src/test/java/com/rakor/xuepinyin/ProgressRuleTest.kt`

- [ ] **Step 1: 写失败测试 `ProgressRuleTest.kt`**

```kotlin
package com.rakor.xuepinyin

import com.rakor.xuepinyin.domain.nextLessonId
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressRuleTest {
    @Test fun `无进度时从第 1 课开始`() {
        assertEquals(1, nextLessonId(emptySet(), 13))
    }

    @Test fun `学到第 4 课时下一课是第 5 课`() {
        assertEquals(5, nextLessonId(setOf(1, 2, 3, 4), 13))
    }

    @Test fun `乱序完成同样取最小未完成`() {
        assertEquals(2, nextLessonId(setOf(1, 3), 13))
    }

    @Test fun `全部完成停在第 13 课`() {
        assertEquals(13, nextLessonId((1..13).toSet(), 13))
    }
}
```

- [ ] **Step 2: 运行验证失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rakor.xuepinyin.ProgressRuleTest"`
Expected: 编译失败（`nextLessonId` 未定义）。

- [ ] **Step 3: 写 `domain/ProgressRule.kt`**

```kotlin
package com.rakor.xuepinyin.domain

/** 当前应学课程 = 最小未完成课号；全部完成则停在最后一课（复习态由调用方判断） */
fun nextLessonId(completedLessonIds: Set<Int>, totalLessons: Int): Int {
    for (id in 1..totalLessons) if (id !in completedLessonIds) return id
    return totalLessons
}
```

- [ ] **Step 4: 运行验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.rakor.xuepinyin.ProgressRuleTest"`
Expected: 4 个测试 PASS。

- [ ] **Step 5: 写 `ui/home/HomeScreen.kt`**

```kotlin
package com.rakor.xuepinyin.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.model.Lesson
import com.rakor.xuepinyin.domain.nextLessonId
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.CandyType
import com.rakor.xuepinyin.ui.theme.candyFocus

@Composable
fun HomeScreen(
    course: Course,
    completedLessonIds: Set<Int>,
    onOpenLesson: (Int) -> Unit,
    onOpenModule: (String) -> Unit,
    onOpenTable: () -> Unit,
    onOpenParent: () -> Unit,
) {
    val currentId = nextLessonId(completedLessonIds, course.lessons.size)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CandyColors.Background)
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        HomeTopBar(onOpenModule, onOpenParent)
        Spacer(Modifier.height(10.dp))
        HeroCard(
            lesson = course.lesson(currentId)!!,
            completed = completedLessonIds.size,
            total = course.lessons.size,
            onStart = { onOpenLesson(currentId) },
        )
        Spacer(Modifier.height(12.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            course.units.forEach { unit ->
                UnitSection(
                    title = unit.title,
                    rangeText = "第 ${unit.lessonRange.first}–${unit.lessonRange.last} 课",
                    lessons = unit.lessonRange.mapNotNull { course.lesson(it) },
                    completedIds = completedLessonIds,
                    currentId = currentId,
                    onOpenLesson = onOpenLesson,
                )
            }
        }
        HomeFooter()
    }
}

@Composable
private fun HomeTopBar(onOpenModule: (String) -> Unit, onOpenParent: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CandyColors.Coral, RoundedCornerShape(10.dp))
                    .border(2.dp, CandyColors.Ink, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("拼", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(8.dp))
            Text("拼音乐园", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
        }
        Spacer(Modifier.width(20.dp))
        listOf("学一学" to "learn", "读一读" to "read", "写一写" to "write", "玩一玩" to "play").forEach { (label, route) ->
            Button(
                onClick = { onOpenModule(route) },
                modifier = Modifier.padding(end = 6.dp).candyFocus(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CandyColors.Sun,
                    contentColor = CandyColors.Ink,
                ),
                shape = RoundedCornerShape(20.dp),
            ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onOpenParent,
            modifier = Modifier.candyFocus(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = CandyColors.InkSoft,
            ),
        ) { Text("家长中心", fontSize = 16.sp) }
    }
}

@Composable
private fun HeroCard(lesson: Lesson, completed: Int, total: Int, onStart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE9B8), RoundedCornerShape(14.dp))
            .border(3.dp, CandyColors.Ink, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (completed == 0) "开始学习 · 第 ${lesson.id} 课" else "继续学习 · 第 ${lesson.id} 课",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA4602A),
            )
            Text(lesson.title, fontSize = 40.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
            Text("已学 $completed / $total 课 · 跟着熊猫奇奇学发音", fontSize = 14.sp, color = CandyColors.InkSoft)
        }
        Text("🐼", fontSize = 52.sp)
        Spacer(Modifier.width(14.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.candyFocus(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CandyColors.Coral,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(14.dp),
        ) { Text("开始学习", fontSize = 20.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun UnitSection(
    title: String,
    rangeText: String,
    lessons: List<Lesson>,
    completedIds: Set<Int>,
    currentId: Int,
    onOpenLesson: (Int) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(CandyColors.Coral, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(rangeText, fontSize = 13.sp, color = CandyColors.InkSoft)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lessons.forEach { lesson ->
                LessonChip(
                    lesson = lesson,
                    state = when {
                        lesson.id in completedIds -> ChipState.DONE
                        lesson.id == currentId -> ChipState.NOW
                        else -> ChipState.TODO
                    },
                    onOpen = { onOpenLesson(lesson.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private enum class ChipState { DONE, NOW, TODO }

@Composable
private fun LessonChip(
    lesson: Lesson,
    state: ChipState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val doneColors = listOf(
        CandyColors.Coral, CandyColors.Sun, CandyColors.Mint, CandyColors.Sky,
    )
    val (bg, content) = when (state) {
        ChipState.DONE -> Color(0xFFE2F7F2) to CandyColors.Ink
        ChipState.NOW -> CandyColors.Coral to Color.White
        ChipState.TODO -> CandyColors.Card to CandyColors.InkSoft
    }
    Button(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().height(64.dp).candyFocus(),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = content),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (state) {
                    ChipState.DONE -> "✓ 第 ${lesson.id} 课"
                    ChipState.NOW -> "第 ${lesson.id} 课"
                    ChipState.TODO -> "第 ${lesson.id} 课"
                },
                fontSize = 11.sp,
            )
            Text(lesson.title, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HomeFooter() {
    Text(
        "◀ ▲ ▼ ▶ 方向键选择 · OK 确认 · 返回退出 · 统编版语文一年级上册",
        fontSize = 13.sp,
        color = CandyColors.InkSoft,
        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
    )
}
```

- [ ] **Step 6: 写 `ui/StubModuleScreen.kt`（后续计划逐个替换）**

```kotlin
package com.rakor.xuepinyin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.CandyType

/** 临时占位页：Plan 2-5 会用真实模块页替换 */
@Composable
fun StubModuleScreen(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(CandyColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = CandyType.title, color = CandyColors.Ink)
            Text("本模块在后续计划中实现", fontSize = CandyType.body, color = CandyColors.InkSoft)
            Button(onClick = onBack) { Text("返回", fontSize = 18.sp) }
        }
    }
}
```

- [ ] **Step 7: 写导航壳 `ui/PinyinApp.kt` 并接入 MainActivity**

```kotlin
package com.rakor.xuepinyin.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rakor.xuepinyin.data.AssetsCourseRepository
import com.rakor.xuepinyin.ui.home.HomeScreen

@Composable
fun PinyinApp(repository: AssetsCourseRepository) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                course = repository.course,
                completedLessonIds = emptySet(), // Plan 2 接入真实进度
                onOpenLesson = { id -> nav.navigate("lesson/$id") },
                onOpenModule = { route -> nav.navigate(route) },
                onOpenTable = { nav.navigate("table") },
                onOpenParent = { nav.navigate("parent") },
            )
        }
        composable(
            route = "lesson/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("id") ?: 1
            StubModuleScreen("第 $id 课 · 学一学", onBack = { nav.popBackStack() })
        }
        composable("read") { StubModuleScreen("读一读", onBack = { nav.popBackStack() }) }
        composable("write") { StubModuleScreen("写一写", onBack = { nav.popBackStack() }) }
        composable("play") { StubModuleScreen("玩一玩", onBack = { nav.popBackStack() }) }
        composable("table") { StubModuleScreen("拼音总表", onBack = { nav.popBackStack() }) }
        composable("parent") { StubModuleScreen("家长中心", onBack = { nav.popBackStack() }) }
    }
}
```

`MainActivity.kt` 的 `setContent` 块替换为：

```kotlin
setContent {
    CandyTheme {
        PinyinApp(repository = AssetsCourseRepository.fromAssets(applicationContext))
    }
}
```

- [ ] **Step 8: 全量构建 + 全部测试**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: 全部测试 PASS，`BUILD SUCCESSFUL`。

- [ ] **Step 9: TV 模拟器 D-pad 冒烟**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.rakor.xuepinyin/.MainActivity`

手动清单（用模拟器方向键盘）：
1. 启动即见首页：顶栏、Hero（第 1 课 a o e）、三个单元 13 张课程卡、底部提示。
2. 方向键在顶栏 → Hero CTA → 课程卡之间可移动，焦点有放大+描边。
3. OK 点「开始学习」进入「第 1 课 · 学一学」占位页，返回键回到首页。
4. 「学一学/读一读/写一写/玩一玩/家长中心」均可进入占位页并返回。

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/ app/src/test/
git commit -m "feat: 首页课程地图（糖果乐园）与导航壳，D-pad 焦点可用"
```

---

## 后续计划系列（本计划完成后依次编写并执行）

| 计划 | 范围 | 交付物 |
|---|---|---|
| Plan 2 | 学一学 5 步流程页 + 进度状态机 + DataStore + 星级奖励页 | 完整学习闭环可玩 |
| Plan 3 | voice-pack-generation 生成语音包 + VoicePackPlayer + 课程/指令语音接入 | 全程有声音 |
| Plan 4 | 读一读两模式 + 写一写笔顺数据（47 字母）与 Canvas 分笔动画 | 四模块补两块 |
| Plan 5 | 玩一玩两个游戏（出题器 TDD）+ 拼音总表 + 家长中心（进度/设置/护眼） | 功能全量 |
| Plan 6 | InputMode（触控/焦点）+ 平板横屏字号档 + 两套 Compose UI 测试配置 + 验收清单 | 平板可用 |

## Self-Review 记录

- 规格覆盖：本计划只覆盖规格的 §2（双入口/横屏/leanback）、§6（主题 Token 与焦点态）、§3（13 课内容表）、§8（lessons.json 首版）与首页；其余章节由 Plan 2-6 覆盖（见上表），无遗漏。
- 无占位符：所有代码步骤均为完整代码；`StubModuleScreen` 与 `completedLessonIds = emptySet()` 是有意的增量交付点，已在代码注释与上表标明替换计划。
- 类型一致性：`Course/Lesson/PinyinLetter/UnitGroup`、`nextLessonId(completedLessonIds, totalLessons)`、`AssetsCourseRepository.fromAssets(context)` 在各任务间签名一致；`LetterType` 四值与 JSON 字符串一致（SINGLE_FINAL/COMPOUND_FINAL/INITIAL/SPECIAL）。
- 测试映射：47 字母数、拼音2=i u ü y w、整体认读 16 个、标调位置抽查均对应规格 §3 的硬性内容要求。
