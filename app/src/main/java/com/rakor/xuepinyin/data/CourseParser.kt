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
