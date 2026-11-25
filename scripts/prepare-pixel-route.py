"""Crop the user's labelled route sheets; remove only connected checkerboard sky.

No resampling, painting, or new image generation: source pixels are preserved.
"""
from pathlib import Path
from collections import deque
from PIL import Image, ImageDraw
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
    # Isolate the supplied train from the platform so it can actually depart.
    train = Image.open(OUTPUT/'underground-front.png').crop((1922, 40, 2649, 365))
    mask = Image.new('L', train.size)
    ImageDraw.Draw(mask).polygon([(0,20),(24,6),(80,2),(726,2),(726,300),
        (310,300),(304,317),(290,323),(88,323),(76,313),(76,294),
        (38,294),(38,262),(5,247),(0,224)], fill=255)
    train.putalpha(mask)
    paint = ImageDraw.Draw(train)
    for left, right in [(157,191),(215,249),(505,533),(562,592)]:
        paint.rectangle((left,62,right,130), fill='#273a39')
        paint.rectangle((left+3,65,right-3,70), fill='#729594')
        paint.line((left+4,113,right-4,91), fill='#476569', width=2)
    for left, right in [(320,445),(669,726)]:
        paint.rectangle((left,62,right,135), fill='#283a3d')
        paint.rectangle((left,65,right,83), fill='#3a5357')
        paint.line((left,98,right,98), fill='#7d8e83', width=3)
        paint.line((left,118,right,118), fill='#526065', width=2)
        paint.polygon([(left+7,92),(left+39,68),(left+46,68),(left+14,92)], fill='#597475')
    train.save(OUTPUT/'metro-car.png')

    entrance = Image.open(OUTPUT/'street-walkway.png').crop((2180,0,2568,341))
    street_mask = Image.new('L', entrance.size)
    ImageDraw.Draw(street_mask).polygon([(89,0),(118,0),(127,8),(155,3),(311,3),
        (330,20),(339,0),(365,0),(366,280),(352,299),(94,299),(87,277)], fill=255)
    from PIL import ImageChops
    entrance.putalpha(ImageChops.multiply(street_mask, entrance.getchannel('A')))
    entrance.save(OUTPUT/'metro-entrance.png')
    (OUTPUT/'sources.json').write_text(json.dumps(manifest, indent=2))

if __name__ == '__main__': main()
