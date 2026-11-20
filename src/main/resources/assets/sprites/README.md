# Sprite assets

The renderer loads these images from the classpath:

- `commuter-atlas.png`: commuter running, jumping, sliding, and confused poses.
- `objects-atlas.png`: dog-poop, e-scooter, pigeon, Navigo-card, and heart sprites.

The current images are cleaned copies of the supplied JPG sheets. The checkerboard was removed to create alpha transparency; the originals were left untouched. The renderer selects frames by source rectangles, so sprites in the atlases do not need equal-sized cells. Replace an atlas with a transparent PNG at the same path to update the art without changing the game logic. If an image or frame is missing or invalid, the game uses its simple fallback drawing.

The Paris scene layers and their current parallax behavior are documented in `../scenes/README.md`. The four-part route image is a design reference for later route transitions, not a runtime asset.
