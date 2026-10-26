# Miaoli

A native Android client for [tribune](https://github.com/seeschloss/miaoli) chat spaces.

This is a from-scratch Kotlin rewrite of the old React Native client. By default it
shows one merged feed from two tribunes:

| Tribune | Color | Time zone | Backend |
| --- | --- | --- | --- |
| Moules | `#FDE096` | `Europe/Paris` | `https://moules.org/board/last.php?backend=tsv` (TSV) |
| LinuxFR | `#67C6F2` | `Europe/Paris` | `https://linuxfr.org/board/index.xml` (XML) |

Server timestamps are interpreted in each tribune's configured time zone and
messages are merged by their absolute UTC instant, so sources in different zones
interleave correctly. Each row shows only that tribune's local clock, in full
`HH:mm:ss` form. Clock references in message text, including
`YYYY-MM-dd HH:mm:ss` forms and optional `@Title` suffixes, are highlighted in
the link colour. The feed refreshes every 30 seconds while the app is in the
foreground.
Use the menu button in the top bar to open the tribune pane. Each row has a switch
for showing or hiding that source in the current session and a pencil icon for its
configuration page. Disabled sources are greyed out, and tribunes hidden from the
feed remain listed so they can be configured or re-enabled.

Tap any message to insert its tribune-local clock at the composer cursor,
replacing the selection if there is one, and focus the message field. With an
empty draft or before the field has a cursor, the clock goes at the end. A
postable origin becomes the composer target; a read-only origin leaves the
current target unchanged. When the target is not a clock's origin, that clock is
shown with an `@Title` suffix, for example `12:34:56@Moules`. Suffixes are
updated when a clock is inserted or the target changes. Each clock keeps its
origin internally, and editing text that overlaps a clock turns it back into
plain text.

## Configuration

Open the side pane and tap the pencil next to a tribune to edit that tribune's
own settings page. Each tribune can be configured independently:

- title and color (`#RRGGBB`)
- backend URL and feed format (TSV or HTML-escaped XML)
- time zone (IANA ID, for example `Europe/Paris` or `UTC`)
- optional post URL and form field name
- user agent
- optional cookie header
- whether the tribune appears in the merged feed

The settings icon in the top bar, or **Feed settings** at the bottom of the side
pane, opens the global polling setting. The interval is measured in seconds,
defaults to 30 seconds, and is clamped to 5–3600 seconds. Settings are stored in
app-private `SharedPreferences` and applied after pressing **Save**. Posting still
works without authentication; a configured cookie is only sent as an optional
header.

## Posting

The composer at the bottom of the feed sends plain text messages without
authentication. It currently targets this tribune on miaoli.im:

```text
https://miaoli.im/@See/+6QbxZZANv8
```

The default target is defined in `data/Tribunes.kt` (`Tribunes.nativeTest`) and
can be changed from the configuration screen. It is deliberately hidden from the
merged feed by default: the Moules and LinuxFR feeds are read-only and remain
unchanged. Enable **Show in feed** for Sikfart if you want to see its posts too.

The request is an `application/x-www-form-urlencoded` POST using the configured
form field (default `message`), `Accept: application/json`, the tribune's user
agent, and its optional cookie header.

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

- `data/` — tribune configuration and persistence, post model, TSV/XML parsers, fetching, merging and posting.
- `ui/` — Compose feed screen, configuration screen, tribune side pane, composer, post row, view model and Material 3 theme.
- `app/src/test/` — parser, merge, config, request and view model tests.

## Not implemented yet

Adding or removing tribunes, interactive login, and navigation between individual
tribunes.
