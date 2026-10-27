# Miaoli

An Android tribune client (colloquially known as a *coincoin*).

Tribunes are a novel (... well they've been around for decades, but not widely known, I guess) type of semi-instant communication space, mostly used in the French free-software community.

The defining feature of tribunes is posts being referenced by their timestamps in answers, making it easy to follow conversations. No explicit federation is used, but posts can have cross-references to clocks on other tribunes (with 12:34:56@tribune) which works seamlessly for users which have this other tribune enabled.

A tribune basically provides a posting address and a TSV or XML-based feed of posts. Many are anonymous, some require logging in.

Miaoli is initially a [tribune engine](https://github.com/seeschloss/miaoli) and a [website providing free tribunes](https://miaoli.im). This Android client works with both Miaoli tribunes and most other tribunes found in the wild (LinuxFR, Euromussels, Sveetch...).

## Basic operation

Add new tribunes if you need to with the configuration side panel. You can log into a tribune from its configuration.

Answer a post by tapping it, which inserts this post's clock in the typing area and switches the destination tribune if necessary.

Tap a clock on an existing post to scroll to the referenced post. Long tap to filter the list and only show posts from this discussion thread.

That's basically all there is to it.

## Future features

Tribune parameters autodiscovery to make it easier and faster to add a new tribune.

Totoz support (totoz are small reaction images hosted on https://totoz.eu).

# Build
## Requirements

- JDK 21
- Android SDK platform 37
- Android SDK build-tools 37.0.0

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
