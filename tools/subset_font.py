# -*- coding: utf-8 -*-
"""LXGW 文楷子集：保留 UI 与课程内容用字，输出 Regular + Medium 两个字重

源字体：LXGW WenKai v1.510 — https://github.com/lxgw/LxgwWenKai/releases（勿混用其他版本重新生成）
用法：python tools/subset_font.py
输出：res/font/wenkai_subset.ttf（Regular，拼音/小字）+ res/font/wenkai_medium_subset.ttf（Medium，大字/标题）
"""
import glob, json, os, sys
from fontTools import subset

HERE = os.path.dirname(os.path.abspath(__file__))
APP = os.path.join(HERE, "..", "app", "src", "main")
FONTS = [
    # (源字体文件, 输出文件)
    ("LXGWWenKai-Regular.ttf", "wenkai_subset.ttf"),
    ("LXGWWenKai-Medium.ttf", "wenkai_medium_subset.ttf"),
]
# 拼音全字母（含四声）+ 数字 + 常用标点，兜底低端电视盒不可预测的 fallback
EXTRA = ("0123456789.%/：，。！？、（）·—…－＋＝→←"
         "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
         "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜüê")

def _keep(ch):
    """只保留会被 UI 渲染的字符；emoji/符号（如 sceneEmoji）不进字体，交由系统渲染或直接不显示"""
    o = ord(ch)
    return (
        o <= 0x7E                # ASCII
        or 0x00C0 <= o <= 0x024F  # 拉丁扩展（拼音声调字母）
        or 0x2000 <= o <= 0x206F  # 通用标点（— … “ ”）
        or 0x2100 <= o <= 0x21FF  # 常用符号与箭头（→ ←）
        or 0x2460 <= o <= 0x24FF  # 带圈数字
        or 0x3000 <= o <= 0x303F  # CJK 标点
        or 0x4E00 <= o <= 0x9FFF  # 汉字
        or 0xFF00 <= o <= 0xFFEF  # 全角符号（＋ ＝ ％）
    )

def collect_chars():
    chars = set(EXTRA)
    # UI 文案：扫描全部 Kotlin 源码里的非 ASCII 字符（含注释，宁多勿缺）
    for path in glob.glob(os.path.join(APP, "java", "**", "*.kt"), recursive=True):
        with open(path, encoding="utf-8") as f:
            for ch in f.read():
                if ord(ch) > 0x7F and _keep(ch):
                    chars.add(ch)
    # 课程内容：lessons.json 全部字符串值（汉字/拼音/例词/例句/口诀/情境词）
    with open(os.path.join(APP, "assets", "lessons.json"), encoding="utf-8") as f:
        def walk(v):
            if isinstance(v, str):
                chars.update(c for c in v if _keep(c))
            elif isinstance(v, list):
                for x in v:
                    walk(x)
            elif isinstance(v, dict):
                for x in v.values():
                    walk(x)
        walk(json.load(f))
    return "".join(sorted(chars))

def subset_one(src, out, needed):
    options = subset.Options()
    options.notdef_outline = True
    font = subset.load_font(src, options)
    s = subset.Subsetter(options)
    s.populate(text=needed)
    s.subset(font)
    subset.save_font(font, out, options)

    # 自检：重新加载输出字体，逐字确认 cmap 覆盖；缺字宁可构建失败，不可静默回退系统字体
    from fontTools.ttLib import TTFont
    cmap = TTFont(out).getBestCmap()
    missing = "".join(c for c in needed if ord(c) not in cmap)
    if missing:
        print("MISSING in %s (%d chars): %s" % (os.path.basename(out), len(missing), missing))
        sys.exit(1)
    print("OK ->", os.path.abspath(out), os.path.getsize(out) // 1024, "KB,",
          len(needed), "chars all covered")

def main():
    needed = collect_chars()
    out_dir = os.path.join(APP, "res", "font")
    os.makedirs(out_dir, exist_ok=True)
    for src_name, out_name in FONTS:
        src = os.path.join(HERE, src_name)
        if not os.path.exists(src):
            sys.exit("缺少源字体 tools/%s（v1.510），下载: https://github.com/lxgw/LxgwWenKai/releases（勿混用其他版本重新生成）" % src_name)
        subset_one(src, os.path.join(out_dir, out_name), needed)

if __name__ == "__main__":
    main()
