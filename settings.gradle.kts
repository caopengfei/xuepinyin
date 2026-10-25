// 阿里云镜像只服务本机（境外直连不可用）；CI（GitHub Actions 注入 CI=true）直连官方源，
// 境外 runner 走镜像慢且偶发 5xx（5xx 会被 Gradle 判为仓库损坏导致解析失败，不会自动换源）。
// pluginManagement 块是独立编译的早执行单元，访问不到脚本顶层 val，故内联同一表达式
pluginManagement {
    repositories {
        if (!providers.environmentVariable("CI").isPresent) {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (!providers.environmentVariable("CI").isPresent) {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "xuepinyin"
include(":app")
