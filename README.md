# Last Métro — Dernière Rame

> Sprint through pixel-art Paris at 1:14 AM and catch the final métro before it disappears.

**Last Métro** is a JavaFX arcade runner with two game modes, configurable controls,
coin trails, score multipliers, and a timed route built from the supplied pixel-art layers.

## Game modes

### Catch the Last Métro

Cover 500 meters before the 45-second timer expires. The run begins with three hearts and ends with the arriving-train sequence when the goal is reached.

### Endless Commute

Run as far as possible, chase a saved high score, and survive a street that becomes faster as the distance increases.

Collect gold Métro coins along flat trails and jump arcs. Every seven coins raises
the multiplier, up to 4×; keep collecting within 2.8 seconds to maintain the streak.
Damage breaks it. Navigo cards grant 100 points and a five-second coin magnet.
Avoid scooters, pigeons, roadwork barriers, wooden crates, and dog poop. Finishing
the 500 m route enters the illuminated station and awards a 500-point train bonus.

Collect the red heart pickups to restore one life, up to a maximum of three. At
full health, a heart awards 50 points instead. Hearts are reachable on the running
lane and appear periodically, with recovery opportunities brought forward after a
hit. Healing uses the same pixel heart as the HUD, plus a chime and a score popup.
A fatal collision ends the run immediately; pickups cannot revive a finished run.

## Movement

The run eases up to speed during the first half-second. Hold jump for a roughly
142 px, 0.72-second arc; release early for a short hop. Jump input buffers for
160 ms before landing. Down fast-falls in the air and continues into a slide when
held. A slide can be cancelled into a jump. Hazard collision edges forgive small
visual grazes; shadows, landing squash, and dust communicate contact.
Animation cadence follows the actual scroll speed, including slowdowns and the
Endless speed ramp. The simulation uses the existing fixed 60 Hz clock.

## Pixel-art route and transfers

The game uses a grounded adult commuter with a brown coat and satchel, six pixel-art
running frames, and separate jump/slide poses. The portrait, flat-palette coins,
barriers, crates, cones, beveled panels, and Press Start 2P type stay in the pixel-art
style. The previous painted/anime experiment is archived under
`raw_assets/illustrated-experiment/` locally and is excluded from Git and the build.

The 45-second active-play budget is divided into three sections:

| Active time | Section | Assets |
| --- | --- | --- |
| 0–20 s | Paris streets | Existing Part 1 far sky, buildings, and walkway |
| 20–33 s | Metro approach | Cropped continuation sky, houses, and sidewalk |
| 33–45 s | Underground platform | Cropped tunnels, platform wall, and foreground tracks |

A 2.4-second transfer screen separates each section. The departure countdown,
world movement, hazards, and power-up clocks pause during transfers. The destination
scene swaps beneath the opaque part of the fade. Inputs and hazards are cleared
before the next section so the runner cannot take an unseen hit. The 500 m finish
uses the supplied platform/train ending. Endless mode continues underground.

Far scenery moves at 8–10% of camera speed, buildings/walls at 35%, and the walkway
at full speed. Repeatable body strips cover each section's duration; the metro
entrance is placed from the section's remaining active time so it reaches the
runner at the boundary. Nearest-neighbour sampling keeps the source pixels crisp.

The supplied labelled sheets are saved under `raw_assets/route-sheets/`.
Run `python scripts/prepare-pixel-route.py` to reproduce the label-free crops and
checkerboard removal. Exact crop rectangles are recorded in
`src/main/resources/assets/pixel-route/sources.json`. The commuter was generated
with the built-in image_gen tool; its prompt is in `docs/PIXEL_ROUTE_ART.json`.

## Atmosphere and feedback

The scene includes drifting leaves and motes, a soft vignette, grounded shadows,
jump/landing dust, slide trails, landing squash, pickup sparkles and score popups,
brief impact shake, spinning gold coins, and victory confetti. The station artwork
includes a lit train interior. Effects follow the simulation and freeze when paused.

Bundled offline audio adds city/train ambience, footsteps, jumps, landings, sliding,
Navigo validation, impacts, countdown warnings, doors, train arrival/departure, and
win/loss cues. English station announcements use a synthetic voice with a filtered
public-address echo. Audio loading runs on a background worker; computers without
a sound output remain playable. No microphone or network speech service is used.

Use **F8** to toggle sound and **F9** for reduced motion from any screen, or click
their rows in Controls & Settings. Reduced motion removes shake, impact flashes,
particles, landing squash, and collectible animation; damage remains visible with
a steady translucent player. Both preferences persist alongside control bindings.
Losing window focus automatically pauses the run; P or Esc resumes it.

## Controls

| Action | Default | Notes |
|---|---|---|
| Jump | `Space` | Rebindable |
| Slide | `Down` | Rebindable |
| Pause | `P` or `Esc` | Reserved in control settings |
| Sound | `F8` | Saved on/off preference |
| Reduced motion | `F9` | Saved accessibility preference |
| Menu | `1`, `2`, `H`, `O` | Catch mode, Endless mode, help, or settings |
| Retry | `R` | Available on the results screen |
| Return | `Esc` or `Enter` | Available from most secondary screens |

Jump and slide bindings can be changed from **Controls & Settings**. Menus and results also support mouse input.

## Requirements

- JDK 21
- Apache Maven 3.9+
- Windows, macOS, or Linux with a graphical desktop

## Run

```bash
mvn javafx:run
```

The game opens in a 1280 × 720 logical viewport and scales proportionally when the window is resized.

## Test

```bash
mvn test
```

The JUnit 5 suite and JaCoCo report run in CI for pushes and pull requests to `main` or `master`.

On Windows, `./scripts/preview-smoke.ps1` runs a desktop integration check and writes
screenshots plus an eight-second PNG frame sequence under `target/preview/`.
It exercises menus, settings, jumps, pickups, impacts, pause/resume, reduced motion,
train arrival, victory, retry, and an Endless defeat. Saved data is isolated under
`target/smoke-home/` so this check does not change player preferences or records.

On a Linux system without a desktop session:

```bash
xvfb-run -a mvn test
```

## Create a Windows build

With JDK 21 and Maven installed, run from PowerShell:

```powershell
./scripts/package-windows.ps1
```

The self-contained application is written to `target/dist/LastMetro/`:

```text
LastMetro.exe
runtime/
```

Players do not need Maven or a separate Java installation. GitHub Actions also publishes a `LastMetro-Windows` artifact for Windows runners.

## Asset pipeline

Clean the raw Part 1 backgrounds with Pillow:

```bash
python3 clean_part1.py
```

This creates four RGBA PNGs in `src/main/resources/assets/backgrounds/`:

- `part1_far_bg.png`
- `part1_mid_buildings_A.png`
- `part1_mid_buildings_B.png`
- `part1_front_walkway.png`

The middle pair receives top-down sky removal, checkerboard fringe dilation, and horizontal seam feathering. The walkway receives magenta keying and edge despill cleanup.

## Project structure

```text
src/main/java/com/lastmetro/
├── MainApp.java              JavaFX entry point and input handling
├── model/                    Pure-Java game simulation
├── animation/                Transitions and frame animation
├── persistence/              High scores and control settings
├── patterns/                 State, strategy, factory, and observer patterns
└── view/                     Canvas rendering and sprite strips

src/main/resources/
├── assets/backgrounds/       Cleaned Part 1 parallax layers
├── assets/scenes/            Earlier scene assets
├── assets/sprites/           Commuter and hazard atlases
└── fonts/                    Press Start 2P font

src/test/java/                JUnit 5 test suite
```

The main scene renderer uses:

- Far sky at 10% camera speed
- Alternating buildings at 35% camera speed
- Walkway, curb, and railing at full camera speed

See [the sprite guide](src/main/resources/assets/sprites/README.md) for atlas layout and replacement notes.

## Saved data

- Endless best score: `~/.lastmetro-highscore`
- Control bindings: `~/.lastmetro/settings.properties`
- Sound and reduced-motion preferences: in the same settings file (older files remain compatible)

## Audio sources

All effects are original synthesized PCM audio. Regenerate them with
`python scripts/generate-audio.py` (standard library only). Regenerate the bundled
announcements with `./scripts/generate-voice.ps1` on Windows (System.Speech,
Microsoft Zira Desktop, and FFmpeg required only for regeneration). The game ships
with the resulting WAV files and needs none of these authoring tools to play.

## Asset fallback

The renderer uses image assets when they load successfully and falls back to simple JavaFX shapes if an asset or sprite frame is unavailable. This keeps the game playable while scene art is being replaced or expanded.
