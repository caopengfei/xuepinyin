import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ---- release 签名 ----
// 取值优先级：仓库根目录 keystore.properties（本机专用，不进库）
//            → Gradle 属性 RELEASE_STORE_FILE / RELEASE_STORE_PASSWORD / RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD（本机放 ~/.gradle/gradle.properties）
// 本仓库不含任何机器路径；都拿不到时 release 构建回退 debug 签名（仅适合自测，日志会告警）。
val releaseKeystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val releaseProp: (String, String) -> String? = { keystoreKey, gradleKey ->
    releaseKeystoreProps.getProperty(keystoreKey)?.trim()?.takeIf { it.isNotEmpty() }
        ?: providers.gradleProperty(gradleKey).orNull?.trim()?.takeIf { it.isNotEmpty() }
}
val releaseStoreFilePath = releaseProp("storeFile", "RELEASE_STORE_FILE")
val releaseStoreFile = releaseStoreFilePath?.let { rootProject.file(it) }?.takeIf { it.exists() }
val releaseStorePassword = releaseProp("storePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseProp("keyAlias", "RELEASE_KEY_ALIAS") ?: "rakor"
val releaseKeyPassword = releaseProp("keyPassword", "RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null && releaseKeyPassword != null
if (!hasReleaseSigning) {
    logger.warn("WARNING: 未找到可用的 release 签名（keystore.properties 或 RELEASE_* 属性），release 包将回退 debug 签名，仅适合自测")
}

android {
    namespace = "com.rakor.xuepinyin"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rakor.xuepinyin"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    signingConfigs {
        // 缺签名信息时不创建该配置，release 构建直接复用 debug 签名
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true   // R8 压缩混淆：课程语音等大体积在 assets，dex 压缩可明显减小 APK
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    androidResources { noCompress += "mp3" }   // MediaPlayer openFd 要求 assets 音频不压缩
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// 产物命名：xuepinyin-v<versionName>.apk（release）；debug 追加 -debug 后缀
// 例：xuepinyin-v0.1.0.apk / xuepinyin-v0.1.0-debug.apk，避免默认的 app-release.apk
androidComponents {
    onVariants { variant ->
        val buildTypeName = variant.buildType ?: "release"
        val suffix = if (buildTypeName == "release") "" else "-$buildTypeName"
        variant.outputs.forEach { output ->
            val impl = output as com.android.build.api.variant.impl.VariantOutputImpl
            val version = output.versionName?.orNull?.replace(Regex("[^0-9A-Za-z._-]"), "-") ?: "unversioned"
            impl.outputFileName.set("xuepinyin-v$version$suffix.apk")
        }
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
    // navigation 2.10+ / datastore 1.3+ 声明 minSdk 24，为支持 minSdk 23（小米盒子4）锁定在 2.9.8 / 1.2.1
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
