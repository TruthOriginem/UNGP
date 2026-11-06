"""为白色图标叠加 UNGP 规则风格的渐变与扫描线着色。

graphics/ungp/icons/ungp_rules 下的图标都是同一套图层样式：白色图形乘上一层自上而下
由亮到暗的着色渐变，再叠一层 8 像素周期的电视扫描线。本脚本复刻这两层效果，用来给
新图标生成与现役图标同风格的正/负/里程碑/黄金版本。

用法::

    # 双击 图标着色.bat（或本脚本）进入交互式向导；也可以显式加 --interactive
    python tools/icon_tint/icon_tint.py --interactive

    # 单个图标 -> ./tinted_green/xxx.png
    python tools/icon_tint/icon_tint.py new_icon.png --color green

    # 批量处理目录（含子目录），输出到指定目录，并生成对照图
    python tools/icon_tint/icon_tint.py art --color red --recursive --out-dir out/red --preview out/red.png

    # 已有彩色图标换成别的色系（先反解成白色底稿再重新着色）
    python tools/icon_tint/icon_tint.py old_green.png --color red --flatten

    # 样式改了以后，从现役图标重新标定渐变和扫描线
    python tools/icon_tint/icon_tint.py --calibrate

只依赖 Pillow（pip install pillow）。
"""

from __future__ import annotations

import argparse
import colorsys
import math
import re
import statistics
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:  # 双击运行时报人话，而不是一屏 traceback
    print("需要 Pillow：python -m pip install pillow")
    raise SystemExit(1)

ROOT = Path(__file__).resolve().parents[2]
REFERENCE_DIR = ROOT / "graphics" / "ungp" / "icons" / "ungp_rules"

# --- 标定数据 ---------------------------------------------------------------
# 16 段 8bit 渐变，由 --calibrate 从现役 ugp_rules 图标反推得到（见 README）。
GREEN_STOPS = [
    (1, 243, 108),
    (1, 237, 104),
    (1, 229, 96),
    (1, 211, 87),
    (1, 208, 80),
    (2, 191, 71),
    (2, 184, 62),
    (3, 171, 53),
    (3, 163, 43),
    (4, 147, 32),
    (4, 138, 25),
    (5, 126, 17),
    (5, 118, 9),
    (5, 110, 4),
    (4, 99, 0),
    (4, 94, 0),
]

RED_STOPS = [
    (244, 38, 37),
    (240, 38, 37),
    (227, 35, 34),
    (223, 35, 35),
    (217, 35, 35),
    (207, 35, 35),
    (200, 37, 37),
    (188, 36, 37),
    (171, 36, 36),
    (156, 36, 36),
    (136, 37, 37),
    (129, 36, 37),
    (125, 35, 35),
    (121, 37, 37),
    (116, 35, 35),
    (94, 28, 27),
]

# 绿色族绿通道的归一化衰减曲线：0 = 顶端最亮，1 = 底端最暗。
# 用于按两个端点颜色生成未标定的色系（紫、金）。
PROGRESS = [0.0, 0.046, 0.085, 0.203, 0.222, 0.333, 0.373, 0.464,
            0.516, 0.621, 0.680, 0.758, 0.810, 0.863, 0.935, 1.0]

# 扫描线：一个周期内各行的乘数（第一个值是最暗行），均值约为 1。
STRIPE_PROFILE = [0.918, 1.030, 1.011, 0.957, 1.063, 0.972, 1.009, 1.041]
STRIPE_PERIOD = 8

# 与 UNGP_RulesManager 中的调色板对应的端点颜色（顶端 / 底端）。
FAMILY_ENDPOINTS = {
    "purple": ((0.72, 0.20, 1.00), (0.28, 0.05, 0.48)),
    "gold": ((1.00, 0.84, 0.00), (0.42, 0.30, 0.00)),
}

FAMILIES = ("green", "red", "purple", "gold")


def _clamp8(value: float) -> int:
    return min(255, max(0, round(value)))


def _ramp_from_endpoints(
    top: tuple[float, float, float], bottom: tuple[float, float, float]
) -> list[tuple[int, int, int]]:
    """按 PROGRESS 曲线在两个端点颜色之间插值，生成 16 段渐变。"""
    stops: list[tuple[int, int, int]] = []
    for p in PROGRESS:
        stops.append(
            (
                _clamp8((top[0] + (bottom[0] - top[0]) * p) * 255),
                _clamp8((top[1] + (bottom[1] - top[1]) * p) * 255),
                _clamp8((top[2] + (bottom[2] - top[2]) * p) * 255),
            )
        )
    return stops


def resolve_ramp(color: str) -> list[tuple[int, int, int]]:
    if color == "green":
        return list(GREEN_STOPS)
    if color == "red":
        return list(RED_STOPS)
    if color in FAMILY_ENDPOINTS:
        return _ramp_from_endpoints(*FAMILY_ENDPOINTS[color])
    raise SystemExit(f"未知色系 {color!r}，可选：{'/'.join(FAMILIES)}")


def build_ramp(stops: list[tuple[int, int, int]], height: int) -> list[tuple[float, float, float]]:
    """把 16 段渐变按行号重采样为 0..1 的浮点颜色。"""
    counts = len(stops)
    rows = []
    for y in range(height):
        pos = 0.0 if height == 1 else y * (counts - 1) / (height - 1)
        low = int(pos)
        high = min(low + 1, counts - 1)
        frac = pos - low
        rows.append(
            tuple(
                (stops[low][c] + (stops[high][c] - stops[low][c]) * frac) / 255.0
                for c in range(3)
            )
        )
    return rows


def build_stripe(height: int, strength: float, period: int, phase: int) -> list[float]:
    """按行生成扫描线乘数；strength 为 0 时不叠扫描线。"""
    if strength <= 0:
        return [1.0] * height
    scaled = max(2, round(period * height / 64))
    count = len(STRIPE_PROFILE)
    rows = []
    for y in range(height):
        index = (((y + phase) * count) // scaled) % count
        rows.append(1.0 + (STRIPE_PROFILE[index] - 1.0) * strength)
    return rows


def _pixels(image: Image.Image) -> bytes:
    return image.convert("RGBA").tobytes()


def _base_index(pixel: tuple[int, int, int], mode: str) -> int:
    if mode == "max":
        return max(pixel)
    return round(0.2126 * pixel[0] + 0.7152 * pixel[1] + 0.0722 * pixel[2])


def tint_image(
    image: Image.Image,
    ramp: list[tuple[float, float, float]],
    stripe: list[float],
    mode: str = "luma",
    gain: float = 1.0,
) -> Image.Image:
    """按 ramp / stripe 给灰度图标着色；alpha 原样保留。"""
    source = image.convert("RGBA")
    width, height = source.size
    if len(ramp) != height or len(stripe) != height:
        raise ValueError("渐变 / 扫描线的行数与图标高度不一致")

    row_lut = []
    for y in range(height):
        r, g, b = ramp[y]
        scale = stripe[y] * gain
        row_lut.append(
            [
                (
                    _clamp8(v * r * scale),
                    _clamp8(v * g * scale),
                    _clamp8(v * b * scale),
                )
                for v in range(256)
            ]
        )

    raw = _pixels(source)
    out = bytearray(len(raw))
    for y in range(height):
        lut = row_lut[y]
        for x in range(width):
            offset = (y * width + x) * 4
            red, green, blue, alpha = raw[offset:offset + 4]
            tint = lut[_base_index((red, green, blue), mode)]
            out[offset:offset + 3] = bytes(tint)
            out[offset + 3] = alpha
    return Image.frombytes("RGBA", (width, height), bytes(out))


def _row_reference(image: Image.Image, channel: int, threshold: int = 8, percentile: float = 0.92) -> list[float | None]:
    """逐行取不透明像素在指定通道上的高分位值；行内像素太少时返回 None。"""
    raw = _pixels(image)
    width, height = image.size
    rows: list[float | None] = []
    for y in range(height):
        start = y * width * 4
        row = [
            raw[i + channel]
            for i in range(start, start + width * 4, 4)
            if raw[i + 3] >= threshold
        ]
        rows.append(_percentile(row, percentile) / 255.0 if len(row) >= 6 else None)
    return rows


def fit_stripe_phase(image: Image.Image, ramp: list[tuple[float, float, float]], channel: int) -> int:
    """估计图标扫描线的相位：与 STRIPE_PROFILE 相关性最高的 0..7。

    同一批行的亮度无论怎么换相位都只是重新分组，集合本身不变，所以"哪一组最暗"这种
    判据是相位不变量、没有意义。这里把每行参考值除以该行的渐变值，按残差类取中位数，
    再和已知 profile 求相关系数——profile 的形状决定了唯一正确的对齐方式。
    """
    rows = _row_reference(image, channel)
    best_phase, best_score = 0, None
    for phase in range(STRIPE_PERIOD):
        buckets: dict[int, list[float]] = {}
        for y, value in enumerate(rows):
            if value is None:
                continue
            buckets.setdefault((y + phase) % STRIPE_PERIOD, []).append(
                value / max(ramp[y][channel], 1e-3)
            )
        pairs = [
            (statistics.median(buckets[k]), STRIPE_PROFILE[k])
            for k in range(STRIPE_PERIOD)
            if len(buckets.get(k, ())) >= 3
        ]
        if len(pairs) < 6:
            continue
        mean_x = statistics.mean(x for x, _ in pairs)
        mean_y = statistics.mean(y for _, y in pairs)
        cov = sum((x - mean_x) * (y - mean_y) for x, y in pairs)
        var_x = sum((x - mean_x) ** 2 for x, _ in pairs)
        var_y = sum((y - mean_y) ** 2 for _, y in pairs)
        denom = math.sqrt(var_x * var_y)
        if not denom:
            continue
        score = cov / denom
        if best_score is None or score > best_score:
            best_phase, best_score = phase, score
    return best_phase


def flatten_image(
    image: Image.Image,
    ramp: list[tuple[float, float, float]],
    phase: int,
) -> Image.Image:
    """把已着色的图标按“输出 = 底稿 × 渐变 × 扫描线”反解回白色底稿。"""
    source = image.convert("RGBA")
    width, height = source.size
    stripe = build_stripe(height, 1.0, STRIPE_PERIOD, phase)
    # 取三个通道里最亮的一个作为底稿亮度：对正常像素等价于原色系的主通道，
    # 但画师额外加的彩色点缀（例如青色宝石）不会被反解成黑色。
    factors = [max(max(ramp[y]) * stripe[y], 1e-3) for y in range(height)]
    raw = _pixels(source)
    out = bytearray(len(raw))
    for y in range(height):
        factor = factors[y]
        for x in range(width):
            offset = (y * width + x) * 4
            value = _clamp8(max(raw[offset], raw[offset + 1], raw[offset + 2]) / factor)
            out[offset] = out[offset + 1] = out[offset + 2] = value
            out[offset + 3] = raw[offset + 3]
    return Image.frombytes("RGBA", (width, height), bytes(out))


def _prepare_base(image: Image.Image, flatten: str, source_family: str | None = None) -> Image.Image | None:
    """返回用于着色的白色底稿；返回 None 表示判断不出原色系，调用方应跳过这张图。

    flatten 取值：
    ``none``    —— 输入本身就是白色底稿，直接上色；
    ``detect``  —— 输入已经着色，按图标色相判断原色系后反解；
    ``auto``    —— 先看饱和度，白底稿直接上色，彩色图标才反解（交互模式用）；
    ``green`` / ``red`` —— 已知原色系，直接反解。
    """
    if flatten == "none":
        return image
    if flatten == "auto" and _mean_saturation(image) <= 0.2:
        return image
    family = flatten if flatten in ("green", "red") else (source_family or _family_of(image))
    if family not in ("green", "red") and flatten == "auto":
        family = _nearest_family(image)  # 交互模式不打断用户，归到最接近的色系
    if family not in ("green", "red"):
        return None
    channel = 1 if family == "green" else 0
    ramp = build_ramp(resolve_ramp(family), image.height)
    phase = fit_stripe_phase(image, ramp, channel)
    return flatten_image(image, ramp, phase)


def _collect_inputs(raw_inputs: list[str], recursive: bool, exclude: Path | None = None) -> list[Path]:
    suffixes = {".png", ".jpg", ".jpeg", ".webp", ".bmp", ".tga"}
    files: list[Path] = []
    for item in raw_inputs:
        path = Path(item)
        if path.is_dir():
            pattern = "**/*" if recursive else "*"
            root = path.resolve()
            # 只有输出目录真的在输入目录内部时才排除它，否则会连输入一起排掉
            skip_root = exclude.resolve() if exclude and root in exclude.resolve().parents else None
            files.extend(
                sorted(
                    p
                    for p in path.glob(pattern)
                    if p.is_file()
                    and p.suffix.lower() in suffixes
                    and (skip_root is None or skip_root not in p.resolve().parents)
                )
            )
        elif path.is_file():
            files.append(path)
        else:
            print(f"[跳过] 找不到 {path}", file=sys.stderr)
    return files


def _mean_saturation(image: Image.Image) -> float:
    """粗略统计不透明像素的平均饱和度，用于提示“这张图已经是彩色的”。"""
    raw = _pixels(image)
    total = 0.0
    count = 0
    for offset in range(0, len(raw), 4):
        if raw[offset + 3] < 8:
            continue
        peak = max(raw[offset], raw[offset + 1], raw[offset + 2])
        if peak < 24:
            continue
        low = min(raw[offset], raw[offset + 1], raw[offset + 2])
        total += (peak - low) / peak
        count += 1
    return total / count if count else 0.0


# 优先用系统中文字体，否则对照图上的中文标签会变成方块
FONT_CANDIDATES = (
    "C:/Windows/Fonts/msyh.ttc",
    "C:/Windows/Fonts/msyhbd.ttc",
    "C:/Windows/Fonts/simhei.ttf",
    "C:/Windows/Fonts/simsun.ttc",
)


def _load_font(size: int = 11):
    """返回 (字体, 是否支持中文)；都失败时返回 (None, False)。"""
    from PIL import ImageFont

    for candidate in FONT_CANDIDATES:
        try:
            return ImageFont.truetype(candidate, size), True
        except Exception:  # 字体不存在或 Pillow 不支持该格式
            continue
    try:
        return ImageFont.load_default(size=size), False
    except Exception:  # 旧版 Pillow 不支持带字号
        return None, False


def build_preview(
    pairs: list[tuple[str, Image.Image, Image.Image]], target: Path, per_row: int = 10
) -> None:
    """生成“原始 / 着色后”并排对照图，图标多时自动换行。"""
    cell = 96
    label = 14
    block = cell * 2 + label * 3
    per_row = max(1, min(len(pairs), per_row))
    bands = (len(pairs) + per_row - 1) // per_row
    columns = min(len(pairs), per_row)
    sheet = Image.new("RGBA", (columns * cell, bands * block), (24, 24, 28, 255))
    draw = ImageDraw.Draw(sheet)
    font, chinese = _load_font()
    before_label, after_label = ("原始", "着色") if chinese else ("origin", "tinted")
    for index, (name, before, after) in enumerate(pairs):
        x = (index % per_row) * cell
        band = (index // per_row) * block
        for row, image in enumerate((before, after)):
            y = band + row * (cell + label)
            thumb = image.convert("RGBA")
            if thumb.size != (cell, cell):
                scale = min(cell / thumb.width, cell / thumb.height)
                thumb = thumb.resize(
                    (max(1, round(thumb.width * scale)), max(1, round(thumb.height * scale))),
                    Image.LANCZOS,
                )
            sheet.alpha_composite(
                thumb, (x + (cell - thumb.width) // 2, y + (cell - thumb.height) // 2)
            )
            draw.text(
                (x + 4, y + cell + 1),
                before_label if row == 0 else after_label,
                fill=(200, 200, 200, 255),
                font=font,
            )
        draw.text(
            (x + 4, band + cell * 2 + label * 2 + 2),
            name[:14],
            fill=(180, 205, 180, 255),
            font=font,
        )
    target.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(target)
    print(f"[预览] {target}（上排原始，下排着色）")


# --- 标定 -------------------------------------------------------------------


def _percentile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    if not ordered:
        return 0.0
    index = min(len(ordered) - 1, max(0, round(fraction * (len(ordered) - 1))))
    return ordered[index]


def _bright_median(image: Image.Image, min_alpha: int) -> list[float] | None:
    """取最亮 40% 像素的中位颜色，作为“这张图标被染成了什么色”的代表。"""
    raw = _pixels(image)
    pixels = [
        (raw[i], raw[i + 1], raw[i + 2])
        for i in range(0, len(raw), 4)
        if raw[i + 3] >= min_alpha
    ]
    if len(pixels) < 40:
        return None
    lums = [max(pixel) for pixel in pixels]
    cut = _percentile(lums, 0.6)
    bright = [pixel for pixel in pixels if max(pixel) >= cut]
    return [statistics.median(pixel[c] for pixel in bright) for c in range(3)]


def _family_of(image: Image.Image) -> str | None:
    """严格判断原色系：色相必须落在实测的红/绿区间内，否则返回 None。"""
    median = _bright_median(image, 255)
    if median is None:  # 半透明图标（例如 wide_radar）没有不透明像素
        median = _bright_median(image, 32)
    if median is None:
        return None
    peak = max(median) or 1
    hue, saturation, _ = colorsys.rgb_to_hsv(*[c / peak for c in median])
    if saturation < 0.15:
        return None
    hue *= 360
    if hue < 40 or hue > 320:
        return "red"
    if 80 < hue < 180:
        return "green"
    return None


def _nearest_family(image: Image.Image) -> str | None:
    """宽松判断：把色相归到最近的已知色系（红 0°、绿 140°），给交互模式兜底。"""
    median = _bright_median(image, 32)
    if median is None:
        return None
    peak = max(median) or 1
    hue, saturation, _ = colorsys.rgb_to_hsv(*[c / peak for c in median])
    if saturation < 0.05:
        return None
    hue *= 360
    return "red" if min(hue, 360 - hue) <= abs(hue - 140) else "green"


def _median_smooth(values: list[float], radius: int = 2) -> list[float]:
    return [
        statistics.median(values[max(0, i - radius):i + radius + 1])
        for i in range(len(values))
    ]


def _fill_gaps(values: list[float | None]) -> list[float]:
    """缺失行用首尾各 10 个有效点的最小二乘斜率外推补齐。"""
    known = [i for i, value in enumerate(values) if value is not None]
    if not known:
        return [0.0] * len(values)
    first, last = known[0], known[-1]
    head = [(i, values[i]) for i in known[:10]]
    tail = [(i, values[i]) for i in known[-10:]]

    def slope_at(points: list[tuple[int, float]]) -> float:
        if len(points) < 2:
            return 0.0
        mean_x = sum(p[0] for p in points) / len(points)
        mean_y = sum(p[1] for p in points) / len(points)
        var = sum((p[0] - mean_x) ** 2 for p in points)
        if var == 0:
            return 0.0
        return sum((p[0] - mean_x) * (p[1] - mean_y) for p in points) / var

    # 渐变自上而下变暗，顶端外推以顶部最亮行为锚点，底端以底部最暗行为锚点
    head_anchor = max(values[i] for i in known[:8])
    tail_anchor = min(values[i] for i in known[-8:])
    head_slope = slope_at(head)
    tail_slope = slope_at(tail)
    result = []
    for i in range(len(values)):
        if values[i] is not None:
            result.append(values[i])
        elif i < first:
            result.append(max(0.0, min(1.0, head_anchor - head_slope * (first - i))))
        else:
            result.append(max(0.0, min(1.0, tail_anchor + tail_slope * (i - last))))
    return result


def _row_samples(image: Image.Image, y: int, channel: int, threshold: int) -> list[int]:
    raw = _pixels(image)
    width = image.width
    start = y * width * 4
    return [
        raw[i + channel]
        for i in range(start, start + width * 4, 4)
        if raw[i + 3] >= threshold
    ]


def calibrate(reference: Path) -> int:
    """从现役图标反推渐变与扫描线，打印可直接粘贴回脚本的数据。"""
    if not reference.is_dir():
        print(f"[错误] 找不到标定目录：{reference}", file=sys.stderr)
        return 1
    icons: dict[str, list[Image.Image]] = {"green": [], "red": []}
    for path in sorted(reference.glob("*.png")):
        image = Image.open(path).convert("RGBA")
        if image.height != 64:
            continue
        family = _family_of(image)
        if family:
            icons[family].append(image)

    merged: dict[str, list] = {}
    for family, images in icons.items():
        if not images:
            continue
        height = 64
        channel = 1 if family == "green" else 0

        ramp: list[list[float]] = []
        for c in range(3):
            values: list[float | None] = []
            for y in range(height):
                samples = []
                for image in images:
                    row = _row_samples(image, y, c, 255)
                    if len(row) >= 6:
                        samples.append(_percentile(row, 0.92))
                values.append(statistics.median(samples) / 255.0 if len(samples) >= 3 else None)
            ramp.append(_median_smooth(_fill_gaps(values)))

        print(f"# {family}: 参与标定的图标 {len(images)} 张")
        print(f"{family.upper()}_STOPS = [")
        for index in range(16):
            pos = index * (height - 1) / 15
            low = int(pos)
            high = min(low + 1, height - 1)
            frac = pos - low
            print(
                "    (%3d, %3d, %3d)," % tuple(
                    _clamp8((ramp[c][low] + (ramp[c][high] - ramp[c][low]) * frac) * 255)
                    for c in range(3)
                )
            )
        print("]")

        profiles = []
        for image in images:
            values: list[float | None] = []
            for y in range(height):
                row = _row_samples(image, y, channel, 255)
                if len(row) < 6:
                    values.append(None)
                    continue
                values.append(_percentile(row, 0.92) / 255.0 / max(ramp[channel][y], 1e-3))
            filled = _fill_gaps(values)
            baseline = statistics.median(filled) or 1.0
            normalised = [value / baseline for value in filled]
            best = None
            for phase in range(8):
                buckets = [
                    statistics.median(
                        [normalised[y] for y in range(height) if (y - phase) % 8 == k]
                    )
                    for k in range(8)
                ]
                spread = max(buckets) - min(buckets)
                if best is None or spread > best[0]:
                    best = (spread, buckets)
            if best and best[0] > 0.10:
                buckets = best[1]
                lowest = buckets.index(min(buckets))
                profiles.append(buckets[lowest:] + buckets[:lowest])

        if profiles:
            averaged = [statistics.median(p[k] for p in profiles) for k in range(8)]
            mean = sum(averaged) / 8 or 1.0
            averaged = [value / mean for value in averaged]
            print("STRIPE_PROFILE = [" + ", ".join(f"{v:.3f}" for v in averaged) + "]")
            print(f"# 扫描线标定用到 {len(profiles)} 张图标")
            merged.setdefault("profiles", []).append(averaged)
        print()

    merged_profiles = merged.get("profiles") or []
    if len(merged_profiles) > 1:
        combined = [
            statistics.median(p[k] for p in merged_profiles) for k in range(STRIPE_PERIOD)
        ]
        mean = sum(combined) / len(combined) or 1.0
        combined = [value / mean for value in combined]
        print("# 两个色系合并后的扫描线，可直接覆盖脚本顶部的 STRIPE_PROFILE：")
        print("STRIPE_PROFILE = [" + ", ".join(f"{v:.3f}" for v in combined) + "]")
    return 0


# --- 命令行 -----------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="给白色图标叠加 UNGP 规则风格的渐变色与扫描线。",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("inputs", nargs="*", help="要处理的图标，或包含图标的目录")
    parser.add_argument(
        "--color", default="green", choices=FAMILIES, help="色系，默认 green"
    )
    parser.add_argument("--out-dir", help="输出目录，默认 ./tinted_<色系>")
    parser.add_argument("--suffix", default="", help="输出文件名追加的后缀，例如 _new")
    parser.add_argument("--recursive", action="store_true", help="递归处理目录")
    parser.add_argument("--gain", type=float, default=1.0, help="整体亮度增益，默认 1.0")
    parser.add_argument("--mode", choices=("luma", "max"), default="luma", help="白度取样方式，默认 luma")
    parser.add_argument("--stripe", type=float, default=1.0, help="扫描线强度倍数，0 为关闭，默认 1.0")
    parser.add_argument("--stripe-period", type=int, default=STRIPE_PERIOD, help=f"扫描线周期基准像素，默认 {STRIPE_PERIOD}")
    parser.add_argument("--stripe-phase", type=int, default=0, help="扫描线相位偏移，默认 0")
    parser.add_argument(
        "--flatten",
        action="store_true",
        help="输入已经是着色过的图标，先反解成白色底稿再换成目标色系",
    )
    parser.add_argument(
        "--from",
        dest="source_family",
        choices=("green", "red"),
        help="配合 --flatten 指定输入的原色系，默认自动判断",
    )
    parser.add_argument("--preview", help="生成对照图到指定路径")
    parser.add_argument("--overwrite", action="store_true", help="覆盖已存在的输出文件")
    parser.add_argument("--dry-run", action="store_true", help="只列出将要写入的文件")
    parser.add_argument(
        "--interactive",
        action="store_true",
        help="进入交互式向导（双击脚本或 图标着色.bat 时自动进入）",
    )
    parser.add_argument(
        "--calibrate",
        nargs="?",
        const=str(REFERENCE_DIR),
        help="从现役图标重新标定并打印数据（默认读取 ugp_rules 目录）",
    )
    args = parser.parse_args(argv)

    if args.calibrate:
        return calibrate(Path(args.calibrate))

    # 双击运行时没有参数、标准输入是控制台，直接进向导
    if args.interactive or (not args.inputs and sys.stdin.isatty()):
        return run_interactive()

    if not args.inputs:
        parser.print_help()
        return 1

    if args.source_family and not args.flatten:
        parser.error("--from 需要和 --flatten 一起使用")
    if not 0 <= args.stripe_phase < max(1, args.stripe_period):
        parser.error("--stripe-phase 必须在 0 到 stripe-period-1 之间")

    ramp_stops = resolve_ramp(args.color)
    out_dir = Path(args.out_dir) if args.out_dir else Path(f"tinted_{args.color}")
    files = _collect_inputs(args.inputs, args.recursive, exclude=out_dir)
    if not files:
        print("没有找到可处理的图标。", file=sys.stderr)
        return 1
    if not args.dry_run:
        out_dir.mkdir(parents=True, exist_ok=True)

    pairs = []
    written = 0
    skipped = 0
    planned: set[Path] = set()
    for path in files:
        try:
            image = Image.open(path).convert("RGBA")
        except Exception:  # 混进目录的非图片文件
            print(f"[跳过] 无法读取 {path}（不是支持的图片格式）", file=sys.stderr)
            skipped += 1
            continue
        saturation = _mean_saturation(image)
        if saturation > 0.2 and not args.flatten:
            print(f"[注意] {path.name} 已有明显颜色（饱和度 {saturation:.2f}），可能是重复着色", file=sys.stderr)
        target = out_dir / f"{path.stem}{args.suffix}.png"
        if target in planned:  # 递归时不同子目录可能有同名文件
            stem, index = target.stem, 2
            while target in planned:
                target = out_dir / f"{stem}_{index}.png"
                index += 1
            print(f"[改名] {path} 与之前输入同名，输出为 {target.name}", file=sys.stderr)
        planned.add(target)
        if target.exists() and not args.overwrite:
            print(f"[跳过] {target} 已存在（用 --overwrite 覆盖）")
            continue
        base = _prepare_base(image, "detect" if args.flatten else "none", args.source_family)
        if base is None:
            print(f"[跳过] {path.name} 判断不出原色系，请用 --from green|red 指定", file=sys.stderr)
            skipped += 1
            continue
        ramp = build_ramp(ramp_stops, image.height)
        stripe = build_stripe(image.height, args.stripe, args.stripe_period, args.stripe_phase)
        tinted = tint_image(base, ramp, stripe, args.mode, args.gain)
        if args.dry_run:
            print(f"[dry-run] {path.name} -> {target}")
        else:
            tinted.save(target)
            written += 1
            print(f"[写入] {target}")
        pairs.append((path.stem, image, tinted))

    if args.preview and pairs and not args.dry_run:
        build_preview(pairs, Path(args.preview))

    if args.dry_run:
        print(f"完成（dry-run）：将写入 {len(pairs)} 个图标 -> {out_dir}")
    else:
        print(f"完成：{written} 个图标 -> {out_dir}" + (f"，跳过 {skipped} 个" if skipped else ""))
    return 0


# --- 交互模式 ---------------------------------------------------------------


FAMILY_LABELS = {
    "green": ("绿色", "正面规则"),
    "red": ("红色", "负面规则"),
    "purple": ("紫色", "里程碑"),
    "gold": ("金色", "黄金里程碑"),
}


def _strip_family_suffix(stem: str) -> str:
    """去掉文件名末尾已有的 _green/_red 之类后缀，避免转色后出现 foo_green_red。"""
    lowered = stem.lower()
    for family in FAMILIES:
        for sep in ("_", "-"):
            tail = f"{sep}{family}"
            if lowered.endswith(tail) and len(stem) > len(tail):
                return stem[: -len(tail)]
    return stem


def _ask(prompt: str, default: str = "") -> str:
    """读一行输入，去掉首尾空白（引号留给 _split_paths 处理）。"""
    raw = input(prompt).strip()
    return raw or default


def _split_paths(raw: str) -> list[str]:
    """拖拽多个文件时资源管理器会给出用引号包起来的路径，逐个拆出来。"""
    quoted = re.findall(r'"([^"]+)"', raw)
    if quoted:
        return quoted
    return [raw.strip().strip("'").strip()]


def _ask_paths() -> list[Path]:
    print("把图标文件或整个文件夹拖进这个窗口再回车（可以拖多个），也可以粘贴路径。")
    while True:
        raw = _ask("图标路径: ")
        if not raw:
            print("  路径不能为空。")
            continue
        paths = [Path(item).expanduser() for item in _split_paths(raw) if item]
        found = [path for path in paths if path.exists()]
        for path in paths:
            if not path.exists():
                print(f"  找不到 {path}")
        if found:
            return found
        print("  请检查路径后重试。")


def _ask_color() -> str:
    print("\n选择要变成的颜色：")
    for index, family in enumerate(FAMILIES, start=1):
        label, note = FAMILY_LABELS[family]
        print(f"  {index}) {label}（{family}）—— {note}")
    aliases: dict[str, str] = {}
    for index, family in enumerate(FAMILIES, start=1):
        label, _ = FAMILY_LABELS[family]
        aliases[str(index)] = family
        aliases[family] = family
        aliases[label] = family
        aliases[label[0]] = family
    while True:
        raw = _ask(f"输入 1-{len(FAMILIES)} 或颜色名（直接回车 = 绿色）: ", "1").lower()
        key = raw[:-1] if raw.endswith("色") else raw
        if key in aliases:
            return aliases[key]
        print(f"  没看懂 {raw!r}，请重新输入。")


def _wizard_jobs(paths: list[Path]) -> list[tuple[Path, Path]]:
    """整理成 (源文件, 输出目录)；文件夹默认只看当前层，没有图片时才递归。"""
    jobs: list[tuple[Path, Path]] = []
    for path in paths:
        if path.is_dir():
            files = _collect_inputs([str(path)], recursive=False)
            if not files:
                files = _collect_inputs([str(path)], recursive=True)
                if files:
                    print(f"  {path} 里没有直接的图片，已改为包含子文件夹。")
            jobs.extend((source, path.resolve()) for source in files)
        else:
            jobs.append((path, path.parent.resolve()))
    return jobs


def _tint_all(jobs: list[tuple[Path, Path]], color: str) -> tuple[int, int]:
    """逐张着色，输出到各自的输入目录，返回 (写入数, 跳过数)。"""
    written = 0
    skipped = 0
    planned: set[Path] = set()
    for source, out_dir in jobs:
        try:
            image = Image.open(source).convert("RGBA")
        except Exception:  # 混进来的非图片文件
            print(f"[跳过] 无法读取 {source.name}（不是支持的图片格式）")
            skipped += 1
            continue
        # 输出文件与输入同目录；已有色系后缀的先剥掉，避免 foo_green_red
        target = out_dir / f"{_strip_family_suffix(source.stem)}_{color}.png"
        if target.resolve() == source.resolve():
            print(f"[跳过] {source.name} 已经是 {color}")
            skipped += 1
            continue
        if target.resolve() in planned:  # 同目录里 foo.png 与 foo_red.png 会转出同一个名字
            print(f"[跳过] {source.name} 的目标 {target.name} 已由前面的图标生成")
            skipped += 1
            continue
        planned.add(target.resolve())
        base = _prepare_base(image, "auto")
        if base is None:
            print(f"[跳过] {source.name} 判断不出原来的色系，请改用命令行 --from green|red 指定")
            skipped += 1
            continue
        ramp = build_ramp(resolve_ramp(color), image.height)
        stripe = build_stripe(image.height, 1.0, STRIPE_PERIOD, 0)
        tinted = tint_image(base, ramp, stripe, "luma", 1.0)
        try:
            tinted.save(target)
        except OSError as error:
            print(f"[跳过] 写不进 {target}（{error}）")
            skipped += 1
            continue
        written += 1
        print(f"[写入] {target}")
    return written, skipped


def _interactive_flow() -> int:
    print("=" * 60)
    print("UNGP 图标着色工具 · 交互模式")
    print("=" * 60)
    print("把图标换成规则/里程碑用的绿、红、紫、金配色，输出保存到输入文件所在目录。")
    print("随时可以按 Ctrl+C 退出。\n")
    paths = _ask_paths()
    jobs = _wizard_jobs(paths)
    if not jobs:
        print("\n没有找到可处理的图片（支持 png/jpg/webp/bmp/tga）。")
        return 1
    if len(paths) == 1 and paths[0].is_dir():
        print(f"\n找到 {len(jobs)} 个图标：{paths[0]}")
    else:
        print(f"\n找到 {len(jobs)} 个图标")
    color = _ask_color()
    out_dirs = sorted({str(out_dir) for _, out_dir in jobs})
    print("\n输出目录：" + "，".join(out_dirs))
    print(f"文件名会加上 _{color} 后缀，不会覆盖你的原始图标。\n")
    written, skipped = _tint_all(jobs, color)
    print(f"\n完成：{written} 个图标" + (f"，跳过 {skipped} 个" if skipped else ""))
    return 0


def _wait_for_close() -> None:
    """双击运行时让窗口停住，否则看不清结果就关掉了。"""
    if not sys.stdin.isatty():
        return
    try:
        input("\n按回车键关闭窗口…")
    except (EOFError, KeyboardInterrupt):
        pass


def run_interactive() -> int:
    """交互式向导：问路径、选颜色，输出到输入文件所在目录。"""
    try:
        return _interactive_flow()
    except (EOFError, KeyboardInterrupt):
        print("\n已取消。")
        return 1
    finally:
        _wait_for_close()


if __name__ == "__main__":
    try:  # 统一 UTF-8 输出；控制台若不是 65001 代码页需要先 chcp 65001
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
    raise SystemExit(main())
