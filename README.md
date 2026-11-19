# Last Métro — Dernière Rame

A small JavaFX endless-runner prototype about a Parisian commuter trying to catch the last métro. The current build focuses on the game engine and uses simple placeholder shapes until the final art is ready.

## Run the game

Install **JDK 21** and **Apache Maven**, then open a terminal in this folder and run:

```sh
mvn javafx:run
```

The window can be resized; the 1280 × 720 game view keeps its proportions and adds letterboxing when needed.

## Play

- **Catch the Last Metro**: cover 500 m before the 45-second service timer expires, then catch the arriving train. You start with three hearts.
- **Endless Commute**: survive as long as possible and set a distance record. The street speeds up as you travel farther.
- Avoid street hazards, collect tickets for bonus points, and pause or retry from the in-game screens.

The main menu accepts `1` for Catch the Last Metro, `2` for Endless, `H` for How to Play, and `O` for Controls & Settings. Jump and Slide can be rebound there. By default, **Space** jumps, **Down** slides, and **P** or **Esc** pauses. The help, pause, and results screens also accept clicks.

## Build and verify

```sh
mvn test
```

The same test command runs in GitHub Actions on pushes and pull requests to `main` or `master`.

## Add the commuter art later

Optional transparent PNG sprite strips go in `src/main/resources/assets/sprites/`:

- `commuter-run.png`: four horizontal frames.
- `commuter-jump.png`: two horizontal frames.
- `commuter-slide.png`: two horizontal frames.

All frames in each image must have equal dimensions. The renderer scales them to the gameplay size. Missing or invalid images keep the placeholder character. See [the sprite guide](src/main/resources/assets/sprites/README.md) for the loading contract.

## Saved data

The Endless best score is saved in `.lastmetro-highscore` under the user's home folder. Control bindings are saved under `.lastmetro/settings.properties` in the same folder.

## Current scope

This is an asset-ready playable foundation, not the final art-complete release. It currently has two game modes, a single route, keyboard controls, mouse-accessible menus, placeholder game-world graphics, and no audio. Additional routes, production artwork, audio, localization, and final distribution packaging can be added in later checkpoints.
