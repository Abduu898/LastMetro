# Optional character sprite strips

The game looks for these optional classpath images:

- `commuter-run.png`: 4 frames in one horizontal row, looped while running.
- `commuter-jump.png`: 2 frames in one horizontal row, used while airborne.
- `commuter-slide.png`: 2 frames in one horizontal row, looped while sliding.

Use transparent PNGs. Every frame in a strip must have the same width and height. The game scales each frame to the commuter's gameplay size, so you can draw at a larger pixel resolution. If a file is missing, unreadable, or has an incompatible width, the colored placeholder remains in use.

Add the images in this folder with the exact filenames above. No code changes are needed for these three character animations.
