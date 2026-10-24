# Miaoli

A native Android client for [tribune](https://github.com/seeschloss/miaoli) chat spaces.

This is a from-scratch Kotlin rewrite of the old React Native client. It currently
shows one merged feed from two hardcoded tribunes:

| Tribune | Color | Backend |
| --- | --- | --- |
| Moules | `#FDE096` | `https://moules.org/board/last.php?backend=tsv` (TSV) |
| LinuxFR | `#67C6F2` | `https://linuxfr.org/board/index.xml` (XML) |

Messages are merged by server timestamp and each row is colored according to its
tribune. The feed refreshes every 30 seconds while the app is in the foreground.

## Requirements

- JDK 21
- Android SDK platform 37
- Android SDK build-tools 37.0.0

On Arch Linux the SDK packages are available from the AUR:

```bash
paru -S android-sdk-cmdline-tools-latest android-sdk-build-tools android-platform
```

The SDK is expected in `/opt/android-sdk`. `local.properties` (untracked) points
Gradle at it:

```properties
sdk.dir=/opt/android-sdk
```

## Build

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleDebug
```

Run unit tests:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:testDebugUnitTest
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Structure

- `data/` — tribune configuration, post model, TSV/XML parsers, fetching and merging.
- `ui/` — Compose feed screen, post row, view model and Material 3 theme.
- `app/src/test/` — parser and merge unit tests.

## Not implemented yet

Posting, clocks/reference navigation, login/cookies, settings, adding tribunes,
persistence and navigation between individual tribunes.
