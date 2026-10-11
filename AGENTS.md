# FreeColor — agent guidelines

Native offline Android water-sort game. Pure Java (no Kotlin sources), Android SDK only,
`minSdk 33` / `targetSdk 34`, AGP + Gradle Wrapper. `README.md` describes behaviour,
architecture and verification; this file describes how to *write* code here.

## Commands

Windows: `gradlew.bat`. POSIX: `./gradlew`.

| Action | Command |
| --- | --- |
| Debug build | `:app:assembleDebug` |
| Unit tests | `:app:testDebugUnitTest` |
| Signed release build | `:app:assembleRelease` |

- JDK: the wrapper needs JDK 17+; the Android Studio bundled JBR works
  (`C:\Program Files\Android\Android Studio\jbr`). On a shell where `java` is not on PATH,
  set `JAVA_HOME` explicitly.
- Outputs: `app/build/outputs/apk/debug/freecolor-debug.apk`,
  `app/build/outputs/apk/release/freecolor-release.apk`.
- Signature check: `apksigner verify --verbose app/build/outputs/apk/release/freecolor-release.apk`.
- Always run `testDebugUnitTest` after touching `core`; it is the project's real safety net.

## Layout

- `app/src/main/java/com/freecolor/core` — game rules, solver, generator, persistence model.
  **No Android imports and no third-party libraries in this package.**
- `app/src/main/java/com/freecolor/ui` — `MainActivity` coordination, `GameView` drawing,
  `GamePersistence`, `VictoryScreen`.
- `app/src/test/java/com/freecolor/core` — JUnit 4 tests for the core package only.
- `app/src/main/res` — native theme, vectors and the launcher icon.

## Style — hard rules

### 1. Every control-flow body is braced. Never single-line.

This is the rule this project enforces and the most frequently broken one. Write:

```java
if (ready) {
    start();
} else {
    retry();
}
for (int i = 0; i < n; i++) {
    sum += values[i];
}
for (int color : tube) {
    counts[color]++;
}
```

Never write any of these:

```java
if (ready) start();                       // single-line body
for (int c : tube) counts[c]++;           // single-line body
if (a) b(); else c();                     // single-line body on either branch
if (ready)                                // unbraced continuation on the next line
    start();
for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) run(i, j);   // chained on one line
```

Applies to `if`, `else`, `else if`, `for`, `while`, `do`, and to the same statements inside
lambdas and anonymous classes. The body may be short — it still gets its own braces and line.

Only exception: an **empty** constructor/method body may stay on one line, e.g.
`private GameConfig() {}`.

### 2. Braces on the same line (K&R / Java convention)

`public void undo() {`, `} else {`, `} catch (JSONException ex) {` — never Allman style.

### 3. Indentation and whitespace

- 4 spaces per level, no tabs anywhere.
- One space after `if`, `for`, `while`, `switch`, `catch` and before `(`:
  `if (cond)`, `for (int i = 0; i < n; i++)`. Never `if(cond)`.
- No space between the method/constructor name and `(`: `solve(candidate)`.
- Spaces around binary operators and after commas: `a = b + 1`, `int i = 0, j = 1`.
- No space inside empty parens: `new int[0]`, `refresh()`.
- Do not re-indent unrelated code and do not convert line endings while editing:
  the tree is currently mixed CRLF/LF and only new files should pick one consistently.

### 4. Line length

There is no enforced limit (existing sources reach ~290 characters for long canvas/persistence
chains). Prefer keeping a line under ~120 characters by wrapping *expressions* over several lines
with continuation indent, not by truncating logic.

### 5. Declaration and class shape

- `final class` for types that are never extended; utility types get a `private` constructor.
- Immutable value holders are `record`s (`LevelSolver.Solution`, `GamePersistence.Saved`).
- Related constants live in `GameConfig` as `public static final` with a short comment above the
  group; magic numbers used in drawing may stay local to `GameView`.
- Fields are assigned in `onCreate`/constructors, not through service locators.

### 6. Naming

- `lowerCamelCase` for methods, locals and fields; `PascalCase` for types;
  `UPPER_SNAKE_CASE` for constants.
- Test methods read as scenarios: `@Test public void timerStartsOnlyAfterTenthUseAndRefillsAtBoundary() {`.

### 7. Language

Identifiers, comments and documentation are in **English**. Strings the user sees are in
**Russian** (`"Отлично!"`, `"Продолжить"`, `"Заново"`). Keep that split.

### 8. Comments

Explain *why* a decision was made (bounded search, animation/commit ordering, visibility of
liquid segments), not what the line does. Keep the existing block-comment voice and do not
delete a comment when you reformat the code around it.

### 9. License header

Every file under `app/src` starts with the same block comment, placed above `package`:

```java
/*
 * Copyright (C) 2026 TermoGRAF
 *
 * This file is part of FreeColor.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
```

Never remove or reword it; copy it verbatim into new sources. The full licence text is
in `COPYING` (GNU GPL v3) plus `COPYING.LESSER` (the LGPLv3 additional permissions) —
together they are LGPLv3. Keep the header English, like every other code comment.

## Dependencies and scope

- The installed app has **no network permission and no runtime dependencies**. Do not add
  libraries, permissions, advertising or payments.
- Test code may use `junit:junit` only; instrumentation runners are not configured.
- Release signing comes from `app/keystore.properties` + `app/keystore/release.jks`, both
  gitignored. Never commit them, never inline passwords, and do not "fix" the intentional
  failure in `app/build.gradle` when `keystore.properties` is missing.
- `local.properties`, `.gradle/`, `**/build/` are machine-local — do not edit or commit them.

## Before you finish

1. `gradlew.bat :app:testDebugUnitTest` passes.
2. Re-scan for one-line control statements — including code you did not write in this session:
   every `if`/`for`/`while`/`else` body must sit inside braces on its own line.
3. Keep the change set local: no whole-file reformatting, no drive-by renames, no line-ending
   churn. Sources here are actively edited by other tools and broad rewrites get lost.
   New `.java` files must carry the license header of rule 9.
4. Behaviour did not change: formatting must not alter evaluation order, add `else` branches,
   or move statements across a `continue`/`break`/`return`.
