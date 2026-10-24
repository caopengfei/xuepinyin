# 拼音乐园 Plan 2/6：学一学（5 步流程）+ 进度存储 + 奖励页 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 替换「学一学」占位页为完整课程学习闭环：每课 5 步（认读→声调→笔顺→拼读→奖励），DataStore 持久化进度，首页接真实进度（继续学习/三态课程卡）。

**Architecture:** `domain/progress` 纯 Kotlin 进度状态机与星级规则（TDD 核心）；`data` 层 `ProgressStore` 接口 + 内存实现（测试）+ Preferences DataStore 实现（JSON 序列化）；`ui/lesson` 步骤机页面 + 复用组件 `FourLineGrid`（四线三格）；`PinyinApp` 接线。

**Tech Stack:** 在 Plan 1 基础上新增 `androidx.datastore:datastore-preferences:1.3.0`、`androidx.lifecycle:lifecycle-runtime-compose:2.9.4`。

**设计依据:** 设计文档 §4/§5.1/§8；星级标准：1★=完成 4 个学习步骤，2★=+跟读完成，3★=+拼读全对。

---

### Task 1: 进度状态机与星级规则（TDD）

**Files:** Create `app/src/main/java/com/rakor/xuepinyin/domain/progress/LessonProgress.kt`；Test `app/src/test/java/com/rakor/xuepinyin/LessonProgressTest.kt`

核心模型与规则：

```kotlin
enum class LessonStep { VIEW, TONES, STROKE, SYLLABLE }
val LESSON_STEPS = LessonStep.entries

@Serializable
data class LessonProgress(
    val stepsDone: Set<LessonStep> = emptySet(),
    val readAlongDone: Boolean = false,
    val syllablesAllCorrect: Boolean = false,
) {
    fun isCompleted(): Boolean = stepsDone.containsAll(LESSON_STEPS)
    fun stars(): Int = when {
        isCompleted() && syllablesAllCorrect -> 3
        isCompleted() && readAlongDone -> 2
        isCompleted() -> 1
        else -> 0
    }
    fun markStep(step: LessonStep) = copy(stepsDone = stepsDone + step)
    fun markReadAlong() = copy(readAlongDone = true)
    fun markSyllables(allCorrect: Boolean) = copy(syllablesAllCorrect = syllablesAllCorrect || allCorrect)
}

@Serializable
data class ProgressState(val lessons: Map<Int, LessonProgress> = emptyMap()) {
    fun completedLessonIds(): Set<Int> = lessons.filterValues { it.isCompleted() }.keys
    fun of(lessonId: Int): LessonProgress = lessons[lessonId] ?: LessonProgress()
    fun update(lessonId: Int, progress: LessonProgress) = copy(lessons = lessons + (lessonId to progress))
    fun totalStars(): Int = lessons.values.sumOf { it.stars() }
}
```

测试用例（先红后绿）：未完成 0 星；4 步完成=1星且 isCompleted；+跟读=2星；+拼读全对=3星；markStep 幂等；markSyllables(true) 后不可被 false 覆盖；ProgressState.completedLessonIds/totalStars 聚合正确。

- [ ] 写测试 → 跑红 → 写实现 → 跑绿 → `git commit -m "feat: 进度状态机与星级规则（TDD）"`

### Task 2: ProgressStore 接口 + 内存实现 + 仓库（TDD）

**Files:** Create `data/ProgressStore.kt`（接口+内存实现）、`data/ProgressRepository.kt`；Test `ProgressRepositoryTest.kt`

```kotlin
interface ProgressStore {
    suspend fun load(): ProgressState
    suspend fun save(state: ProgressState)
}
class InMemoryProgressStore(initial: ProgressState = ProgressState()) : ProgressStore {
    private var state = initial
    override suspend fun load() = state
    override suspend fun save(state: ProgressState) { this.state = state }
}
class ProgressRepository(private val store: ProgressStore) {
    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state.asStateFlow()
    suspend fun initialize() { _state.value = store.load() }
    suspend fun update(lessonId: Int, transform: (LessonProgress) -> LessonProgress) {
        val next = _state.value.update(lessonId, transform(_state.value.of(lessonId)))
        _state.value = next; store.save(next)
    }
}
```

测试：initialize 载入；update 写穿到 store 且 StateFlow 通知。

- [ ] 写测试 → 红 → 实现 → 绿 → commit `feat: 进度仓库与存储抽象（TDD）`

### Task 3: DataStore 实现 + 依赖

**Files:** Modify `app/build.gradle.kts`（+datastore、+lifecycle-runtime-compose）；Create `data/DataStoreProgressStore.kt`

Preferences DataStore（key "progress_json"，kotlinx.serialization 编解码；坏数据重置为空进度）。无本地单测（薄层），由 Task 2 接口测试 + 模拟器冒烟覆盖。

- [ ] 实现 → `./gradlew :app:assembleDebug` 通过 → commit `feat: DataStore 进度持久化`

### Task 4: FourLineGrid 与笔顺口诀数据

**Files:** Create `ui/lesson/FourLineGrid.kt`、`domain/StrokeOrderHints.kt`

- `FourLineGrid(modifier, bandContent)`：画四线三格（4 线 3 格，中格内容槽），线色与描边由参数给出；后续写一写/总表复用。
- `StrokeOrderHints.hintOf(pinyin: String): String`：47 字母笔顺口诀（单韵母/声母逐笔说明；复韵母/er 给「依次书写 x 与 y」组合说明），lesson 5 的 k 蝌蚪、üe 例词等终审携带项一并修正时机在 Plan 4。

- [ ] 实现 + 编译通过 → commit `feat: 四线三格组件与笔顺口诀`

### Task 5: LessonScreen 五步页面

**Files:** Create `ui/lesson/LessonScreen.kt`（含 5 个步骤私有 Composable）

- 参数：`lesson: Lesson, initial: LessonProgress, onProgress: (Int, LessonProgress.() -> LessonProgress) -> Unit, onExit: () -> Unit, onOpenNext: (Int) -> Unit`
- 内部状态：`stepIndex`、字母下标、选中声调、拼读答题结果；进入步骤完成时上报 `markStep`；步骤②「跟读」按钮上报 `markReadAlong`；步骤④全部答对上报 `markSyllables`。
- 步骤①认读：左/右切字母，四线三格大字（韵母红/声母蓝），口诀+情境图。
- 步骤②声调：四声芯片选中→大字变调（复用 Plan 1 原型交互），听一听/跟读按钮。
- 步骤③笔顺：四线三格 + 口诀文字 + 「下一笔」步进高亮（动画在 Plan 4）。
- 步骤④拼读：内容优先级 `lesson.syllables` → 整体认读卡片 → 四声操练卡；两拼展示 声母蓝+韵母红=音节+汉字；答题式：显示声母韵母，4 个候选音节选对亮绿。
- 步骤⑤奖励：星星结算（⭐×n）、糖果庆祝、按钮：再学一遍（回步骤①不清进度）/ 下一课 / 返回首页。
- 底部动作条：上一步/下一步；标题条：单元·课名 + 步骤进度点。

- [ ] 实现 → assembleDebug 通过 → commit `feat: 学一学五步课程页`

### Task 6: 接线与冒烟

**Files:** Modify `ui/PinyinApp.kt`（ProgressRepository 参数化、home 读 state、lesson 路由接 LessonScreen、完成课返回首页后进度刷新）、`MainActivity.kt`（构造 DataStoreProgressStore）

- [ ] 全量测试 + assembleDebug → TV 模拟器冒烟：进入第 1 课走完 5 步拿 1 星（做跟读+拼读全对=3 星）→ 返回首页课程卡 1 变已学、Hero 变「继续学习·第 2 课」→ 重启应用进度仍在（DataStore 持久化）→ commit `feat: 首页接入真实进度与学一学闭环`

## Self-Review

- 规格覆盖：设计 §5.1（5 步流程、字母轮播、声调、笔顺口诀、拼读、星级）与 §8 进度模型（steps/跟读/全对）→ Task 1/2/3/5；首页进度接线 → Task 6。
- 占位处理：笔顺动画明确划归 Plan 4（步骤③当前为口诀+步进文字）；音频播按钮 Plan 3 接真声。
- 类型一致性：LessonStep 四值与 LessonProgress.stepsDone、markStep 使用一致；ProgressRepository.update 与 LessonScreen.onProgress 签名对齐。
