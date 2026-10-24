# 拼音乐园 Plan 4/6：读一读 + 写一写（笔顺动画）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: subagent-driven-development / executing-plans。Steps use `- [ ]`。

**Goal:** 替换读一读/写一写占位页：读一读 = 模式选择 + 跟读课本（按课，复用 lesson 语音 key）+ 词句跟读（拼读词，新增 read 语音单元）；写一写 = 47 字母选择 + 四线三格 Canvas 分笔动画。

**Architecture:** `domain/stroke/StrokeData.kt` 手工定义 29 个基础字母（6 单韵母+23 声母）的分笔贝塞尔路径（四线三格归一化坐标 0..1）；复韵母与 zh/ch/sh 由组件字母横向压缩拼合（覆盖全部 47）。`ui/write/`：字母选择页 + StrokeCanvas（PathMeasure 逐笔揭示动画）。`ui/read/`：模式选择页 + 跟读页。语音包扩展：`tools/gen_voice_content.py` 增发 `read` 单元（各课拼读词 ~40 条汉字 TTS），重跑管线五阶段。

**设计依据:** 设计文档 §5.2（两模式）、§5.3（47 字母笔顺、四线三格、OK 逐步/自动播放）、§8（笔顺=分段路径指令）。

---

### Task 1: StrokeData 29 字母分笔路径 + 组合（TDD）
- Create `domain/stroke/StrokeData.kt`；Test `StrokeDataTest.kt`
- API：`object StrokeData { fun strokesOf(pinyin: String): List<Stroke> }`；`Stroke = List<Seg>`；`Seg = M(x,y)/L(x,y)/C(c1x,c1y,c2x,c2y,x,y)`（归一化坐标）。
- 组合规则：zh/ch/sh/复韵母/er → 组件字母依序横排（每组件宽 1/n，段坐标平移缩放）。
- 测试：课程 47 字母全部非空；单字母笔画数与笔顺口诀一致（a=2,o=1,i=2,m=3,x=2…）；组合字母 strokes 数 = 组件之和。
- [ ] 测试红 → 实现 → 绿 → commit

### Task 2: 写一写页面（选择 + Canvas 动画）
- Create `ui/write/WriteScreen.kt`（按课分组的字母选择）+ `ui/write/StrokeScreen.kt`（四线三格 Canvas：已完成笔画实色、当前笔 PathMeasure 揭示动画；按钮：上一笔/下一笔/自动演示）
- [ ] 实现接线 `write` 路由 → assembleDebug + 手动冒烟 → commit

### Task 3: 读一读页面（模式选择 + 跟读）
- Create `ui/read/ReadScreen.kt`：模式卡（跟读课本/词句跟读）；跟读课本 = 选课 → 行列表（呼读行 audio=`lesson{N}/{p}`、口诀行 audio=`_mn`）；词句跟读 = 全部拼读词行（音节+汉字，audio=`read/{i}`）
- [ ] 实现接线 `read` 路由 → commit

### Task 4: 语音包扩展 read 单元
- Modify `tools/gen_voice_content.py`（增 `read` 单元：lessons 依序拼读词去重汉字行）→ 重跑 prepare/合成(airvoz 批)/转码/manifest/校验
- VoicePackAssetsTest 自动覆盖新 key
- [ ] 管线全绿 → 模拟器冒烟（读一读播放）→ commit + merge main

## Self-Review
- 规格覆盖：§5.2 两模式（儿歌已砍）、§5.3 全 47 字母 + OK 逐步 + 自动播放、§8 笔顺路径数据、§9 read key 规则。
- 增量点：平板触控差异（字号档/涟漪）留 Plan 6；游戏与总表留 Plan 5。
