"""Normalize generated demo portraits into production-shaped avatar seed files."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--montage", type=Path, required=True)
    parser.add_argument("images", nargs="+", help="email=source.png")
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    normalized: list[tuple[str, Image.Image]] = []
    for item in args.images:
        email, source = item.split("=", 1)
        with Image.open(source) as image:
            image = ImageOps.exif_transpose(image).convert("RGB")
            side = min(image.size)
            left = (image.width - side) // 2
            top = (image.height - side) // 2
            image = image.crop((left, top, left + side, top + side))
            image = image.resize((512, 512), Image.Resampling.LANCZOS)
            destination = args.output / f"{email}.jpg"
            image.save(destination, "JPEG", quality=88, optimize=True, progressive=True)
            normalized.append((email, image.copy()))

    cell_width, cell_height = 280, 320
    columns = 4
    rows = (len(normalized) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * cell_width, rows * cell_height), "#eaf7f5")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default(size=16)
    for index, (email, image) in enumerate(normalized):
        x = (index % columns) * cell_width
        y = (index // columns) * cell_height
        preview = image.resize((256, 256), Image.Resampling.LANCZOS)
        sheet.paste(preview, (x + 12, y + 12))
        draw.text((x + 12, y + 280), email, fill="#0f172a", font=font)
    args.montage.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.montage, "PNG", optimize=True)


if __name__ == "__main__":
    main()
