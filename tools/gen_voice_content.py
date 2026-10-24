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
            push(pinyin, call)
            if letter.get("mnemonic"):
                push(f"{pinyin}_mn", letter["mnemonic"])
            for i, tone in enumerate(letter["tones"]):
                push(f"{pinyin}_tone{i}", tone)

        units.append({"id": uid, "lines": lines})
        for key, line_no in unit_keys:
            keys[key] = {"unit": uid, "line": int(line_no)}

    units.append({"id": "common", "lines": COMMON})
    for i in range(1, len(COMMON) + 1):
        keys[f"common/{i}"] = {"unit": "common", "line": i}
    units.append({"id": "reward", "lines": REWARD})
    for i in range(1, len(REWARD) + 1):
        keys[f"reward/{i}"] = {"unit": "reward", "line": i}

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
