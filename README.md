# FreeColor

Native offline Android Water Sort game (Java, Android SDK, minimum API 33).

Code style, package rules and the commands expected before finishing a change are in
`AGENTS.md`.

## Open in Android Studio

Select **File > Open**, choose the project directory and let Gradle synchronize.
Use Android Studio's bundled JDK 17 or newer. The project includes the Gradle Wrapper,
AGP 9.4.1 and Gradle 9.6.0. Install Android SDK Platform 34 if missing. `local.properties`
is machine-specific; Android Studio can recreate it for another computer.
Before Gradle sync, copy `app/keystore.properties.example` to `app/keystore.properties` locally
and provide the signing file and credentials. This configuration is required by the build script
even for debug builds; the template contains no real secrets.

Build: `./gradlew :app:assembleDebug`
Tests: `./gradlew :app:testDebugUnitTest`
Windows: use `gradlew.bat`.
Debug APK: `app/build/outputs/apk/debug/freecolor-debug.apk`.
Release APK: `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/freecolor-release.apk`.
Development dependency downloads may require internet on a new machine; the installed
game has no network permission, network dependency, advertising or payments.

## Release signing

`app/build.gradle` reads `app/keystore.properties` (`storeFile`, `storePassword`,
`keyAlias`, `keyPassword`) and signs the `release` build type with it. Both
`keystore.properties` and `app/keystore/` are gitignored; without them
`assembleRelease` fails with an explicit error. The current keystore is a
self-signed RSA-4096 key (`freecolor`, 30 years) created with `keytool -genkeypair`
(JKS). Keep it backed up: without it, later builds cannot be installed as updates of
the already installed application, and a replacement key would have to be published as
a different application.

## Structure and responsibilities

- `app/src/main/java/com/freecolor/core`: immutable GameState, Move, Rules,
  GameEngine (undo and restart), GameConfig, LevelValidator, LevelSolver, LevelGenerator,
  ExtraTubeCooldown (global extra-tube timer) and UndoAllowance (global Undo budget).
  This package has no Android imports.
- `app/src/main/java/com/freecolor/ui`: MainActivity coordinates a background generation
  executor, GamePersistence stores versioned JSON in private SharedPreferences,
  GameView renders responsive glass tubes and animates pours with Canvas/ValueAnimator,
  VictoryScreen draws the full-screen result page.
- `app/src/main/res`: native theme, vector controls and original adaptive launcher artwork.
- `app/src/test`: six JUnit 4 classes (22 tests) covering core rules, history and undo,
  completion, extra-tube cooldown, hidden visibility and generation/solution replay.

## License

Copyright (C) 2026 TermoGRAF. The sources are distributed under the GNU Lesser General
Public License version 3 (LGPLv3). `COPYING.LESSER` carries the LGPLv3 additional
permissions and `COPYING` the GNU GPL v3 text that LGPLv3 incorporates and refers to;
the two files together are the complete licence. Every Java source file under `app/src`
starts with the standard header naming the author and the licence — keep it in place and
copy it into new files.

## Application identity

Application ID: `com.freecolor`. APKs: `freecolor-debug.apk` and `freecolor-release.apk`.
The release certificate uses alias `freecolor`. Completed migration tooling has been removed;
existing private progress remains compatible and unchanged.

## Launcher artwork

Built-in imagegen artwork: `app/src/main/res/drawable-nodpi/launcher_art.png`.
Adaptive icon inset preserves the illustration across circular and rounded-square masks.
Prompt: Square full bleed midnight violet background, chunky transparent test tube with luminous cyan, hot pink and golden yellow liquid layers, a curling golden droplet over its mouth; tactile 3D casual-game style, clean silhouette, no text or border.

## Repository hygiene

Commit sources, resources, tests, Gradle configuration and the complete Gradle Wrapper
(including its JAR), signing template and license texts. Local SDK/IDE files, Gradle caches,
build outputs, signing credentials and keys are excluded by `.gitignore`. Keep both current
and historical signing keys backed up outside version control. The locally generated
`gradle/gradle-daemon-jvm.properties` pins this machine's JDK and is intentionally excluded;
other machines should select a compatible installed JDK through Android Studio or JAVA_HOME.
