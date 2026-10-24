# 拼音乐园 Plan 6/6：平板适配 + 护眼提醒 + 验收 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: subagent-driven-development / executing-plans。Steps use `- [ ]`。

**Goal:** 设备自适应（TV 焦点基准 / 平板触控降一档字号）、20 分钟护眼提醒弹层、全量验收收尾。

**实现要点:**
- `ui/Device.kt`：`resolveInputMode()`（UiModeManager TELEVISION 或无触摸屏 → FOCUS；否则 TOUCH）+ `rememberInputMode()`。
- `CandyTheme`：TOUCH 模式以 `Density(density, fontScale×0.85)` 包裹内容——所有 sp 字号整体降档，dp 布局不变（触控目标已 ≥48dp）；焦点视觉（放大/描边）仅键盘/D-pad 触发，触控天然不显示。
- `PinyinApp`：护眼提醒（settings.eyeRestEnabled 开启时每 20 分钟弹全屏遮罩 + common/6 语音，按钮继续学习）。
- [x] 代码实现与构建/测试通过

**验收清单:**
- [x] 全部单元测试通过（52+）
- [x] TV 模拟器回归：首页/课程/写一写正常
- [x] 触控设备（手机横屏代理平板）：TOUCH 模式字号档生效、可点按操作
- [x] 记忆与文档收尾更新
