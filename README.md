# WearOS Serpent

**Snake Classic** for **Wear OS** watches: the classic snake game, in Kotlin and
Jetpack Compose for Wear. Steer, eat the pellet, grow, and try not to bite yourself
or the wall. Everything runs on the watch: no phone, no network, no account.

This is a port of [AmazfitSerpent](https://github.com/dchernykh1984/AmazfitSerpent),
the same game as a Zepp OS mini app. The rules, the pacing, the layout proportions
and the eleven translations are carried over unchanged; the implementation is new.

## Playing it

- **Board** - a 15x15 grid inscribed in the round screen, with the score in the cap
  above it and the pause control in the cap below.
- **Controls** - four **arrows** in the segments around the board, which on a round
  watch are dead space the board can never use; or **swipe** anywhere to turn. A
  reversal into your own neck is refused rather than fatal. **Back** during a game
  pauses rather than leaving, so a hard turn cannot cost you the run; from a menu it
  steps back, and from the start screen it closes the app as usual.
- **Difficulty** - Slow, Normal or Fast, picked on the start screen. Whichever level
  you played last is the one that opens next time. The snake also creeps faster as it
  grows, down to a floor per level.
- **High score** - kept per difficulty, so beating your record on Fast means
  something. A game that scored nothing never counts as a record.
- **A perfect game** - filling every cell of the board is a win, not a crash.
- **Languages** - English, Russian, German, French, Italian, Spanish, Portuguese,
  Dutch, Polish, Czech and Kazakh. The watch's own language is followed, and all
  eleven are offered individually in the system per-app language list - so Kazakh,
  which Zepp OS had no device-language code for and could never select, finally
  reaches the people it was translated for.

## Devices

Round watches, **Wear OS 3 (API 30) and newer**. Built and tested against a
**OnePlus Watch 2R** (466x466 round, Wear OS 5). The layout is derived from the
screen size at runtime rather than from a device list, so any round watch gets the
same game with correctly sized cells.

## Setup

```bash
git clone https://github.com/dchernykh1984/WearOSSerpent.git
cd WearOSSerpent
```

A JDK 17 and the Android SDK (compileSdk 36) are all that is needed; Gradle comes
with the repository through the wrapper. Point the build at your SDK with a
`local.properties` holding `sdk.dir=/path/to/Android/sdk`, or export `ANDROID_HOME`.

## Develop

```bash
./gradlew testDebugUnitTest   # the JVM unit tests
./gradlew koverVerify         # unit tests + the coverage floor
./gradlew ktlintCheck         # formatting
./gradlew detekt              # static analysis
./gradlew lintDebug           # Android Lint, including the Wear OS checks
./gradlew assembleDebug       # build the APK
./gradlew connectedDebugAndroidTest   # instrumented tests (needs a watch or emulator)
```

The whole pull-request gate in one line, which is exactly what CI runs:

```bash
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest koverVerify assembleDebug assembleRelease
```

Install a debug build on a watch over ADB (pair it over Wi-Fi first, in the watch's
developer options):

```bash
./gradlew installDebug
```

### Layout of the code

```
wear/                                  the one module: the watch app is the product
  src/main/AndroidManifest.xml         watch-only, standalone, no permissions
  src/main/java/com/dchernykh/serpent/
    MainActivity.kt                    the single activity
    SerpentViewModel.kt                the state the screen draws, and the tick loop
    game/Snake.kt                      the rules: movement, growth, collisions, food
    game/SpeedLevel.kt                 the difficulty levels and the tick pacing
    game/Records.kt                    what counts as a record
    layout/RoundGeometry.kt            chord maths that keeps content off the bezel
    layout/BoardLayout.kt              the grid inscribed in the round screen
    layout/ControlLayout.kt            where the arrows and the score sit around it
    layout/Arrow.kt                    the control icons, as line segments
    store/RecordStore.kt               the records, on Preferences DataStore
    ui/                                the Compose screens
  src/main/res/values*/strings.xml     the screen strings, a table per language
  src/main/res/xml/                    what backup and device transfer may take
  src/main/res/mipmap-*/               the adaptive launcher icon
  src/test/                            JVM unit tests - the rules and the geometry
  src/androidTest/                     instrumented tests - what needs a real device
config/detekt/detekt.yml               static-analysis overrides
gradle/libs.versions.toml              every dependency and plugin version
```

The rule that shapes it: anything a test can reach without a device - the rule set,
the pacing, the record decision, the round-screen geometry - is a plain Kotlin class
outside the Compose layer, and `koverVerify` holds it to a coverage floor. Only what
genuinely needs a device is exempt from that floor, and each exemption is written
down where it is made, with the instrumented test that covers it instead. (The floor
is 0 until the game itself lands; there is nothing here yet to hold up.)

## Pre-commit hooks (contributors)

```bash
uv tool install pre-commit   # or: pipx install pre-commit
pre-commit install
pre-commit install --hook-type commit-msg --hook-type pre-push
```

On commit: whitespace and line endings, YAML/TOML/XML well-formedness, a non-ASCII
guard on source and config (translations in `res/values-*/` are exempt - that is
what they are for), and a check that apostrophes in string resources are escaped,
which is an aapt2 error rather than a warning. On the commit message: Conventional
Commits. On push: ktlint, detekt and the unit tests, which need the JDK and the
Android SDK and so are kept off the per-commit path.

## Continuous integration and releases

Every pull request must pass: pre-commit, `actionlint`, commitizen (Conventional
Commits), the Gradle gate above, a CodeQL analysis, an OSV dependency scan and the
instrumented tests on two Wear OS emulators.

Releases are automated with `release-please`: it maintains a version-bump PR from
the Conventional Commits and, when merged, tags a GitHub Release. The release build
then produces a **signed APK**, verifies its signature, records a build-provenance
attestation and attaches the APK and its R8 mapping file to the release.

Signing needs four repository secrets - `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Pull-request CI never touches them: without
`KEYSTORE_FILE` in the environment the release build simply stays unsigned.

Verify a published APK came from this repository:

```bash
gh attestation verify wearos-serpent-<version>.apk --repo dchernykh1984/WearOSSerpent
```

### Dependency locking

`wear/gradle.lockfile` pins every transitive version, so a build today and a build
in a year resolve the same graph. After changing a dependency, regenerate it with
the **Update lockfiles** workflow (or `./gradlew :wear:dependencies --write-locks`)
and commit the result.

## License

Released under the [MIT License](LICENSE).
