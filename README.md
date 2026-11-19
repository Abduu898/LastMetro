# Last Métro — Dernière Rame

A JavaFX endless-runner about a Parisian commuter trying to catch the last métro. The current build combines the game engine with the first supplied character, hazard, collectible, and Paris-scene art.

## Run the game

Install **JDK 21** and **Apache Maven**, then open a terminal in this folder and run:

```sh
mvn javafx:run
```

The window can be resized; the 1280 × 720 game view keeps its proportions and adds letterboxing when needed.

## Play

- **Catch the Last Metro**: cover 500 m before the 45-second service timer expires, then catch the arriving train. You start with three hearts.
- **Endless Commute**: survive as long as possible and set a distance record. The street speeds up as you travel farther.
- Avoid street hazards, collect Navigo cards for bonus points, and pause or retry from the in-game screens.

The main menu accepts `1` for Catch the Last Metro, `2` for Endless, `H` for How to Play, and `O` for Controls & Settings. Jump and Slide can be rebound there. By default, **Space** jumps, **Down** slides, and **P** or **Esc** pauses. The help, pause, and results screens also accept clicks.

## Build and verify

```sh
mvn test
```

The same test command runs in GitHub Actions on pushes and pull requests to `main` or `master`.
On Linux systems without a desktop session, run `xvfb-run -a mvn test` so JavaFX can load and inspect the image assets.

## Create a Windows build

Install **JDK 21** and **Apache Maven**, then run this in PowerShell:

```powershell
./scripts/package-windows.ps1
```

The self-contained app image is created in `target/dist/LastMetro/`. Start it with `LastMetro.exe`; players do not need Maven or a separate Java installation. Each push to `main` or `master` also creates a downloadable `LastMetro-Windows` artifact in the GitHub Actions run. The artifact is a Windows app folder, not an installer, and is currently built for Windows only.

## Art assets

The first art checkpoint is in `src/main/resources/assets/`:

- `sprites/commuter-atlas.png`: running, jumping, sliding, and confused poses.
- `sprites/objects-atlas.png`: animated street hazards, Navigo cards, and heart icons.
- `scenes/paris-composite.jpg`: the supplied Paris scene, cropped by the renderer to hide its board labels.
- `fonts/PressStart2P-Regular.ttf`: pixel display lettering for menu and results headings (SIL Open Font License; see `fonts/OFL.txt`).

The atlas images use transparent backgrounds and source-rectangle animation, so frames can have different sizes. If an image or frame cannot be loaded, the renderer falls back to simple shapes. See [the sprite guide](src/main/resources/assets/sprites/README.md) for the asset layout and replacement notes.

## Saved data

The Endless best score is saved in `.lastmetro-highscore` under the user's home folder. Control bindings are saved under `.lastmetro/settings.properties` in the same folder.

## Current scope

This is an early playable build, not a finished release. It has two game modes, one route, keyboard controls, mouse-accessible menus, a first pass of supplied art, and no audio yet. Separate parallax layers, route transitions, more character and hazard animation, audio, localization, and installer packaging are later checkpoints.
