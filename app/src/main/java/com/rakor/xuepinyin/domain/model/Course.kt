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
