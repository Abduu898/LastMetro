# Last Métro — Dernière Rame

> Sprint through pixel-art Paris at 1:14 AM and catch the final métro before it disappears.

**Last Métro** is a JavaFX arcade runner with two game modes, configurable controls, animated sprites, and a three-layer Paris street scene.

## Game modes

### Catch the Last Métro

Cover 500 meters before the 45-second timer expires. The run begins with three hearts and ends with the arriving-train sequence when the goal is reached.

### Endless Commute

Run as far as possible, chase a saved high score, and survive a street that becomes faster as the distance increases.

Collect Navigo cards for points and avoid dog poop, e-scooters, and swooping pigeons. Hazard sprites face toward the runner.

## Controls

| Action | Default | Notes |
|---|---|---|
| Jump | `Space` | Rebindable |
| Slide | `Down` | Rebindable |
| Pause | `P` or `Esc` | Reserved in control settings |
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

## Asset fallback

The renderer uses image assets when they load successfully and falls back to simple JavaFX shapes if an asset or sprite frame is unavailable. This keeps the game playable while scene art is being replaced or expanded.
