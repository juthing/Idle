# Idle

A screen time manager for Android. Set your own limits, and make lifting them take a real
gesture — scanning a code, tapping a tag, or walking somewhere.

Idle is the first app in a suite of minimal Android tools: free, no ads, no tracking.

## What it does

**Periods** block chosen apps during chosen hours. Social media from 10 pm to 7 am on weekdays,
for instance. A period that crosses midnight belongs to the day it starts on.

**Timers** cap how long a group of apps may be used each day. Apps grouped under one timer share
a single budget, so "two hours of video a day" means two hours across all of them.

**Unlock methods** are what lift a block, and every rule requires one:

- a **QR code or barcode** you scan again;
- an **NFC tag** you tap again;
- a **place** you have to physically be in.

An unlock lasts fifteen minutes by default and covers only the app you were trying to open, not
every app under the rule. A rule that is currently doing its job cannot be edited, disabled or
deleted without going through its unlock method — otherwise the whole app could be defeated by
switching a rule off on impulse. Creating rules is always free, and everything stays readable.

There is a daily **emergency quota** of five minutes, shared across all blocked apps, that needs
no unlock. It cannot be raised or disabled: a quota you could top up would be the simplest way
around every rule you set.

## Privacy

Idle does not declare the `INTERNET` permission. It cannot send anything anywhere, by
construction — not analytics, not crash reports, not your usage.

The accessibility service reads one thing: the package name of the app that just came to the
foreground. It cannot read screen content, what you type, or anything inside your apps.

Unlock secrets are stored as a SHA-256 digest, never as the scanned value, so reading the
database does not let anyone reproduce a code they do not have. Backups are disabled: blocking
state and unlock secrets have no business travelling to another device.

Locations are read on demand only — once when you register a place, once when you unlock. Idle
never subscribes to location updates and never registers a geofence.

## Building

Requires JDK 17+ and the Android SDK (platform 37.2, build-tools 36).

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew installDebug           # install on a connected device
./gradlew testDebugUnitTest      # unit tests covering the blocking logic
./gradlew connectedDebugAndroidTest  # instrumented tests (Room)
./gradlew lint                   # Android Lint
```

Point `local.properties` at your SDK with `sdk.dir=/path/to/android-sdk`.

## Languages

English and French. No other locales are packaged.

## Contributing

`CLAUDE.md` documents the architecture, the conventions and the reasoning behind the technical
decisions. Read it before changing anything structural.

## Licence

See [LICENSE](LICENSE).
