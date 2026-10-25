# 拼音乐园

# kotlinx.serialization：R8 full mode 下 Json 反射查找 Companion.serializer() 需要保留
# 涉及包：课程数据与设置（data/CourseParser、SettingsStore）、语音包清单（audio/VoicePack）、
#        学习进度（domain/progress/LessonProgress、ProgressState）
-keepattributes *Annotation*, InnerClasses

-keepclassmembers class com.rakor.xuepinyin.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.rakor.xuepinyin.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rakor.xuepinyin.data.**$$serializer { *; }

-keepclassmembers class com.rakor.xuepinyin.audio.** {
    *** Companion;
}
-keepclasseswithmembers class com.rakor.xuepinyin.audio.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rakor.xuepinyin.audio.**$$serializer { *; }

-keepclassmembers class com.rakor.xuepinyin.domain.progress.** {
    *** Companion;
}
-keepclasseswithmembers class com.rakor.xuepinyin.domain.progress.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rakor.xuepinyin.domain.progress.**$$serializer { *; }
