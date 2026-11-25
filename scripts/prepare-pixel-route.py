"""Crop the user's labelled route sheets; remove only connected checkerboard sky.

No resampling, painting, or new image generation: source pixels are preserved.
"""
from pathlib import Path
from collections import deque
from PIL import Image
import json

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'raw_assets/route-sheets'
OUTPUT = ROOT / 'src/main/resources/assets/pixel-route'

def checker_alpha(image, name):
    image = image.convert('RGBA')
    w, h = image.size
    px = image.load()
    seen = bytearray(w * h)
    queue = deque()
    def neutral(x, y):
        r, g, b, a = px[x, y]
        return a == 0 or (min(r, g, b) >= 120 and max(r, g, b) - min(r, g, b) <= 21)
    for x in range(w):
        if neutral(x, 0): queue.append((x, 0)); seen[x] = 1
    while queue:
        x, y = queue.popleft()
        r, g, b, _ = px[x, y]; px[x, y] = (r, g, b, 0)
        for nx, ny in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)):
            if 0 <= nx < w and 0 <= ny < h and not seen[ny*w+nx] and neutral(nx, ny):
                seen[ny*w+nx] = 1; queue.append((nx, ny))
    # Enclosed checker areas (rail gaps, window openings) cannot reach the top edge.
    # Keep this key inside the sheet's sky/opening band, above the solid walkway.
    cutoff = {'street-buildings.png': 330, 'street-walkway.png': 180,
              'underground-wall.png': 115, 'underground-front.png': 210}[name]
    threshold = 165 if name.startswith('underground') else 120
    for y in range(min(h, cutoff)):
        for x in range(w):
            r,g,b,a = px[x,y]
            if min(r,g,b) >= threshold and max(r,g,b)-min(r,g,b) <= 12:
                px[x,y] = (r,g,b,0)
    return image

def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    crops = [
        ('street-sheet.png','street-far.png',(92,75,2660,407),False),
        ('street-sheet.png','street-buildings.png',(92,500,2660,1048),True),
        ('street-sheet.png','street-walkway.png',(92,1142,2660,1483),True),
        ('transfer-sheet.png','transfer.png',(74,163,2678,770),False),
        ('transfer-sheet.png','platform-ending.png',(77,869,2676,1489),False),
        ('underground-sheet.png','underground-far.png',(51,219,2700,568),False),
        ('underground-sheet.png','underground-wall.png',(51,652,2700,1000),True),
        ('underground-sheet.png','underground-front.png',(51,1095,2700,1497),True),
    ]
    manifest = []
    for source, name, box, key in crops:
        image = Image.open(SOURCE/source).convert('RGBA').crop(box)
        if key: image = checker_alpha(image, name)
        image.save(OUTPUT/name)
        manifest.append(dict(source=source, output=name, crop=list(box), connected_checker_removed=key))
        print(name, image.size)
    # Reuse foliage only to cap the foreground tree cut by the sheet edge.
    crown = Image.open(OUTPUT/'street-walkway.png').convert('RGBA').crop((479,0,743,82))
    cp = crown.load()
    for y in range(crown.height):
        for x in range(crown.width):
            r,g,b,a=cp[x,y]
            if g < r+5 or g < b+5: cp[x,y]=(r,g,b,0)
    crown.save(OUTPUT/'leaf-crown.png')
    (OUTPUT/'sources.json').write_text(json.dumps(manifest, indent=2))

if __name__ == '__main__': main()
