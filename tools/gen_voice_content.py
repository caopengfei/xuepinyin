#!/usr/bin/env python3
"""从 lessons.json 派生语音包台词（单一事实源）。

产物：
  tools/voice_content.json                      管线内容（units × lines）
  app/src/main/assets/voicepack/keys.json       语义 key -> {"unit","line"}
  tools/voice_script.txt                        人读对照稿

行序（lesson 单元内）：每字母依次为 呼读行、口诀行、（韵母）四声行。
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
LESSONS = ROOT / "app/src/main/assets/lessons.json"

# 声母呼读音（带调拼音，Azure 神经声按音节朗读）
INITIAL_CALL = {
    "b": "bō", "p": "pō", "m": "mō", "f": "fō",
    "d": "dē", "t": "tē", "n": "nē", "l": "lē",
    "g": "gē", "k": "kē", "h": "hē",
    "j": "jī", "q": "qī", "x": "xī",
    "z": "zī", "c": "cī", "s": "sī",
    "zh": "zhī", "ch": "chī", "sh": "shī", "r": "rì",
    "y": "yī", "w": "wū",
}

COMMON = [
    "小朋友，跟熊猫奇奇一起学拼音吧！",
    "按 OK 键试试吧。",
    "跟我读。",
    "你真棒！",
    "再想一想，没关系。",
    "休息一下，保护眼睛。",
]

REWARD = [
    "本课完成啦！",
    "你得到了一颗星。",
    "两颗星，真厉害！",
    "三颗星，你太棒了！",
]

# airvoz 对单独的 i/u/ü（含四声）返回空音频，改用同音汉字作 TTS 输入（读音相同，key 不变）；
# ui/iu/ie/üe 的独立拼写不是合法音节，TTS 会拆成字母读，同样换同音常用字。
# üe 的二、三声在普通话中无对应汉字（该音节只有一、四声），用音节拼写 yué/yuě 让 TTS 按音节+声调配读。
TTS_TEXT_OVERRIDES = {
    "i": "衣", "ī": "衣", "í": "姨", "ǐ": "椅", "ì": "意",
    "u": "乌", "ū": "乌", "ú": "无", "ǔ": "五", "ù": "物",
    "ü": "迂", "ǖ": "迂", "ǘ": "于", "ǚ": "雨", "ǜ": "玉",
    "uī": "威", "uí": "围", "uǐ": "伟", "uì": "位",
    "iū": "优", "iú": "邮", "iǔ": "有", "iù": "又",
    "iē": "椰", "ié": "爷", "iě": "也", "iè": "夜",
    "üē": "约", "üé": "yué", "üě": "yuě", "üè": "月",
}


def main() -> None:
    course = json.loads(LESSONS.read_text(encoding="utf-8"))
    units: list[dict] = []
    keys: dict[str, dict] = {}

    for lesson in course["lessons"]:
        uid = f"lesson{lesson['id']}"
        lines: list[str] = []
        unit_keys: list[tuple[str, str]] = []  # (key, line_no)

        def push(key_suffix: str, text: str) -> None:
            lines.append(text)
            unit_keys.append((f"{uid}/{key_suffix}", str(len(lines))))

        for letter in lesson["letters"]:
            pinyin = letter["pinyin"]
            call = letter["tones"][0] if letter["tones"] else INITIAL_CALL[pinyin]
            push(pinyin, TTS_TEXT_OVERRIDES.get(call, call))
            if letter.get("mnemonic"):
                push(f"{pinyin}_mn", letter["mnemonic"])
            for i, tone in enumerate(letter["tones"]):
                push(f"{pinyin}_tone{i}", TTS_TEXT_OVERRIDES.get(tone, tone))

        units.append({"id": uid, "lines": lines})
        for key, line_no in unit_keys:
            keys[key] = {"unit": uid, "line": int(line_no)}

    units.append({"id": "common", "lines": COMMON})
    for i in range(1, len(COMMON) + 1):
        keys[f"common/{i}"] = {"unit": "common", "line": i}
    units.append({"id": "reward", "lines": REWARD})
    for i in range(1, len(REWARD) + 1):
        keys[f"reward/{i}"] = {"unit": "reward", "line": i}

    # 词句跟读：全部拼读词（与课程顺序一致；应用侧 ReadScreen 用同样的 flatMap 顺序）
    words = [s["word"] for lesson in course["lessons"] for s in lesson["syllables"] if s.get("word")]
    units.append({"id": "read", "lines": words})
    for i in range(1, len(words) + 1):
        keys[f"read/{i}"] = {"unit": "read", "line": i}

    total = sum(len(u["lines"]) for u in units)
    (ROOT / "tools/voice_content.json").write_text(
        json.dumps(units, ensure_ascii=False, indent=1), encoding="utf-8")
    out_keys = ROOT / "app/src/main/assets/voicepack/keys.json"
    out_keys.parent.mkdir(parents=True, exist_ok=True)
    out_keys.write_text(json.dumps({"keys": keys}, ensure_ascii=False, indent=1), encoding="utf-8")

    script = [f"# 语音包对照稿（共 {total} 条）\n"]
    for u in units:
        script.append(f"\n## {u['id']}（{len(u['lines'])} 条）")
        for i, line in enumerate(u["lines"], 1):
            script.append(f"{i}. {line}")
    (ROOT / "tools/voice_script.txt").write_text("\n".join(script), encoding="utf-8")

    print(f"ok: {len(units)} units, {total} lines, {len(keys)} keys")


if __name__ == "__main__":
    main()
