from pathlib import Path
from collections import deque

from PIL import Image


ROOT = Path(__file__).resolve().parent
RAW_DIR = ROOT / "raw_assets"
OUTPUT_DIR = ROOT / "src" / "main" / "resources" / "assets" / "backgrounds"


def is_neutral_sky(r, g, b):
    return (
        abs(r - g) < 28
        and abs(g - b) < 28
        and abs(r - b) < 28
        and r > 105
    )


def clean_mid_building(source_name, output_name):
    image = Image.open(RAW_DIR / source_name).convert("RGBA")
    pixels = image.load()
    width, height = image.size
    transparent = set()

    # Seed only from the top edge, then remove matching neutral pixels connected
    # to that sky. Interior gray details are not cleared unless they touch the sky.
    queue = deque()
    for x in range(width):
        r, g, b, _ = pixels[x, 0]
        if is_neutral_sky(r, g, b):
            queue.append((x, 0))

    while queue:
        x, y = queue.popleft()
        if (x, y) in transparent:
            continue

        r, g, b, alpha = pixels[x, y]
        if alpha == 0 or not is_neutral_sky(r, g, b):
            continue

        transparent.add((x, y))
        if x > 0:
            queue.append((x - 1, y))
        if x + 1 < width:
            queue.append((x + 1, y))
        if y > 0:
            queue.append((x, y - 1))
        if y + 1 < height:
            queue.append((x, y + 1))

    for x, y in transparent:
        r, g, b, _ = pixels[x, y]
        pixels[x, y] = (r, g, b, 0)

    # Dilate the alpha mask once so JPEG dithering on one-pixel square borders
    # is removed as well instead of remaining as faint grid fringes.
    border = set()
    for x, y in transparent:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < width and 0 <= ny < height and (nx, ny) not in transparent:
                    border.add((nx, ny))

    for x, y in border:
        r, g, b, _ = pixels[x, y]
        pixels[x, y] = (r, g, b, 0)

    # Feather only the two architectural tile seams. This removes a hard
    # vertical cut while preserving the interior and base of each image.
    feather_width = 5
    for y in range(height):
        for x in range(feather_width):
            alpha = pixels[x, y][3]
            pixels[x, y] = (
                pixels[x, y][0],
                pixels[x, y][1],
                pixels[x, y][2],
                alpha * (x + 1) // (feather_width + 1),
            )

        for x in range(width - feather_width, width):
            alpha = pixels[x, y][3]
            progress = width - x
            pixels[x, y] = (
                pixels[x, y][0],
                pixels[x, y][1],
                pixels[x, y][2],
                alpha * progress // (feather_width + 1),
            )

    image.save(OUTPUT_DIR / output_name, format="PNG", optimize=False)


def is_bright_magenta(r, g, b):
    return r > 140 and b > 140 and g < min(r, b) - 35


def is_pink_fringe(r, g, b):
    return r > 140 and b > 140 and g < min(r, b) - 25


def clean_walkway(source_name, output_name):
    image = Image.open(RAW_DIR / source_name).convert("RGBA")
    pixels = image.load()
    width, height = image.size
    keyed = set()

    for y in range(height):
        for x in range(width):
            r, g, b, _ = pixels[x, y]
            if is_bright_magenta(r, g, b):
                keyed.add((x, y))

    # Remove a small one-pixel fringe surrounding keyed areas while leaving
    # streetlamp and bush pixels that are not itself magenta/fringe-colored.
    fringe = set()
    for x, y in keyed:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < width and 0 <= ny < height and (nx, ny) not in keyed:
                    r, g, b, _ = pixels[nx, ny]
                    if is_pink_fringe(r, g, b):
                        fringe.add((nx, ny))

    for x, y in keyed | fringe:
        r, g, b, _ = pixels[x, y]
        pixels[x, y] = (r, g, b, 0)

    # Despill semi-transparent pink edge pixels that remain around fine details.
    for y in range(height):
        for x in range(width):
            r, g, b, alpha = pixels[x, y]
            if 0 < alpha < 255 and is_pink_fringe(r, g, b):
                r = min(r, g)
                b = min(b, g)
                pixels[x, y] = (r, g, b, alpha)

    image.save(OUTPUT_DIR / output_name, format="PNG", optimize=False)


def convert_far_background(source_name, output_name):
    image = Image.open(RAW_DIR / source_name).convert("RGBA")
    image.save(OUTPUT_DIR / output_name, format="PNG", optimize=False)


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    convert_far_background("part1_far_bg.png.jpg", "part1_far_bg.png")
    clean_mid_building("part1_mid_buildings_A.png.jpg", "part1_mid_buildings_A.png")
    clean_mid_building("part1_mid_buildings_B.png.jpg", "part1_mid_buildings_B.png")
    clean_walkway("part1_front_walkway.png.jpg", "part1_front_walkway.png")


if __name__ == "__main__":
    main()
