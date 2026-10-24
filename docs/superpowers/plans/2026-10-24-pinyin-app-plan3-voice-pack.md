# 拼音乐园 Plan 3/6：语音包管线 + VoicePackPlayer 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: subagent-driven-development / executing-plans。Steps use `- [ ]`。

**Goal:** 应用有声音：47 字母呼读音、24 韵母四声（96 条）、47 条口诀、UI 指令与奖励语（约 200 条），预打包单声道 Opus 进 assets，`VoicePackPlayer` 按 key 播放并接入学一学各按钮。

**Architecture:** 生成侧（Python，tools/）：`gen_voice_content.py` 从 lessons.json 派生 `tools/voice_content.json`（管线内容）与 `app/src/main/assets/voicepack/keys.json`（语义 key → 单元/行号），单一事实源保证一致；随后走 voice-pack-generation 技能五阶段（prepare 多音字门 → airvoz 页内合成（晓双，儿童声；失败回退 edge-tts 晓晓）→ 转码 Opus → manifest → 校验），产物落 `assets/audio/{unit}/line{N}.opus`。应用侧（Kotlin）：`VoicePack`（keys+manifest 加载与 key→(unit,line) 解析，TDD）、`VoicePackPlayer`（MediaPlayer、打断即播、缺 key 静默降级）、PinyinApp 接线。

**关键决策：** 拼音读音行用带调拼音文本（如 `bā`、`zhī`）作为 TTS 输入——Azure 神经声把带调拼音按音节朗读；汉字行（口诀/指令/奖励语）走多音字门+替换表。呼读音映射：b→bō p→pō m→mō f→fō d→dē t→tē n→nē l→lē g→gē k→kē h→hē j→jī q→qī x→xī z→zī c→cī s→sī zh→zhī ch→chī sh→shī r→rì y→yī w→wū；韵母呼读=一声（tones[0]）。

**设计依据:** 设计文档 §9（语音包管线、台词 key 规则、缺失降级）。

---

### Task 1: 台词生成器（单一事实源）

**Files:** Create `tools/gen_voice_content.py`；产物 `tools/voice_content.json`、`app/src/main/assets/voicepack/keys.json`、`tools/voice_script.txt`（对照稿）

- 单元组织：`lesson{1..13}`（每字母：呼读行 + 口诀行 + 四声行(韵母)）、`common`（6 条 UI 指令）、`reward`（4 条）。
- key 规则：`lesson{N}/{pinyin}`（呼读）、`lesson{N}/{pinyin}_mn`（口诀）、`lesson{N}/{pinyin}_tone{0..3}`、`common/{i}`、`reward/{i}`。
- - [ ] 实现 → 运行生成 → 校验 keys 与 content 行数一致 → commit

### Task 2: 多音字门通过

**Files:** Create `tools/tts_substitutions.json`（confirmedDefault：奇/得/了/一等按门提示迭代补齐）

- [ ] `python <skill>/scripts/prepare_tts_input.py --content tools/voice_content.json --subs tools/tts_substitutions.json` → 200 行 ready

### Task 3: 合成（airvoz 优先，edge-tts 回退）

- [ ] browser-use 打开 `https://airvoz.com/zh/text-to-speech/voice/Xiaoshuang`，页内 runBatch（≤3 并发、每批 ≤30 条）→ base64 解码至 `build/voice-staging/{id}/line{N}.mp3`
- [ ] 若 airvoz 不可用：`edge-tts`（晓晓 zh-CN-XiaoxiaoNeural，需代理）直写 staging
- - [ ] 全部 200 条落盘（>1KB + MP3 魔数）

### Task 4: 转码 + manifest + 校验

- [ ] `transcode_to_opus.py` → `assets/audio/{id}/line{N}.opus`（libopus 单声道 24k）
- [ ] `build_manifest.py` → 各单元 manifest.json（时长）
- [ ] `validate_voice_pack.py` 全绿 → commit 语音资产（约 1-2MB）

### Task 5: VoicePack 解析器（TDD）

**Files:** Create `audio/VoicePack.kt`；Test `VoicePackTest.kt`

```kotlin
class VoicePack(private val keys: Map<String, KeyRef>, private val manifests: Map<String, VoiceManifest>) {
    data class KeyRef(val unit: String, val line: Int)
    fun contains(key: String): Boolean
    fun fileOf(key: String): String?   // "audio/lesson1/line3.opus"
    fun durationMsOf(key: String): Long?
    companion object {
        fun fromAssets(context: Context): VoicePack   // 读 assets/voicepack/keys.json + 各单元 manifest
    }
}
```

- [ ] 测试：key 解析/缺 key/manifest 时长读取（测试注入字符串 JSON）→ 红 → 实现 → 绿 → commit

### Task 6: VoicePackPlayer + 接线

**Files:** Create `audio/VoicePackPlayer.kt`；Modify `ui/PinyinApp.kt`（onPlayAudio → player.play(key)）

- MediaPlayer 打断即播（reset+setDataSource(assets fd)+prepare+start）、缺 key 静默跳过、release。
- LessonScreen 已有 onPlayAudio 调用点（听一听/声调按钮）；口诀呼读在认读页自动播一次（进入字母时）。
- 单测：key 全覆盖校验（lessons.json 派生的每个 key 在 keys.json 中存在——资产完整性测试）。
- [ ] 实现 → 全测 → 模拟器冒烟（logcat 无异常、MediaPlayer start 日志）→ commit + merge main

## Self-Review
- 规格覆盖：§9 台词清单（字母四声+口诀+指令+奖励）、key 规则、debug 校验、缺失降级、TV/平板共用 → Task 1/5/6；Opus→AAC 兜底为运行期异常时的人工预案（管线保留）。
- 风险声明：带调拼音 TTS 读音正确性需抽听；对照稿 tools/voice_script.txt + 单条重生成配方兜底。
