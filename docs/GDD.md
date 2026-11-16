# Game Design Document (GDD): Last Métro — Ligne 13 Edition

## 1. Concept & Pitch
- **Genre:** 2D Side-Scrolling Endless Runner (Satirical / Arcade)
- **Perspective:** 2D Side-View with fixed horizontal player X-position and scrolling Parisian obstacles.
- **Core Premise:** It is 1:14 AM in Paris. You are sprinting toward the station entrance to catch the last Métro train before the 45-second countdown hits `00:00`. Regardless of how well you dodge obstacles, the game ends with a scripted unwinnable finale where the doors slam shut inches from your face.

## 2. Controls & Player Mechanics
- **Jump (`SPACE` / `UP`):** Launches the commuter upward with gravity physics to clear low ground hazards.
- **Slide / Duck (`DOWN`):** Cuts the commuter's vertical hitbox in half (`80px` -> `40px`) to pass beneath aerial hazards.
- **Player States (State Pattern):**
  - `RunningState`: Default ground movement.
  - `JumpingState`: Airborne physics governed by vertical velocity and gravity.
  - `SlidingState`: Reduced hitbox height while grounded.
  - `DebuffedState`: Triggered by stepping in dog poop (slows horizontal speed / inverts Jump & Slide inputs for 3 seconds).

## 3. Entities & Hazards
All game objects inherit from an abstract `Entity` class (`x`, `y`, `width`, `height`, `getBounds()`).
- **Dog Poop (`DogPoop`):** Static ground hazard (`StaticGroundStrategy`). Applies the `Stench/Slippery` debuff on contact.
- **Abandoned E-Scooter (`EScooter`):** High-speed ground hazard (`FastChargeStrategy`) moving faster than the scroll speed. Requires jumping.
- **Swooping Pigeon (`Pigeon`):** Aerial hazard (`AerialSwoopStrategy`) flying at head height. Requires sliding.
- **Double Espresso (`EspressoItem`):** Collectible power-up granting 4 seconds of speed boost and obstacle immunity.

## 4. Architecture & Design Patterns
- **Strict MVC Separation:** `com.lastmetro.model` contains zero JavaFX dependencies, enabling 100% deterministic JUnit 5 unit testing and JaCoCo coverage tracking.
- **State Pattern:** Manages `Commuter` transitions (`Running`, `Jumping`, `Sliding`, `Debuffed`).
- **Strategy Pattern:** Encapsulates obstacle movement behaviors (`MovementStrategy`).
- **Factory Pattern:** `HazardFactory` spawns randomized waves of obstacles and scales difficulty as the countdown approaches zero.
- **Observer Pattern:** `GameEventListener` decouples game events (debuffs, sarcastic Parisian pop-ups, `00:00` finale) from the JavaFX UI renderer.