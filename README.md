# Grounded

Grounded is a calm, location-aware Android companion for understanding recent earthquake activity. It presents official USGS data as a map and readable list, keeps the last successful result available offline, and can add approximate distance context without storing location history.

Grounded is informational. It is not an earthquake early-warning or emergency-safety system.

## Requirements

- Android Studio 2026.1 or compatible
- JDK 17+
- Android SDK 36
- An Android 12 (API 31) or newer device/emulator

## Build

```shell
./gradlew assembleDebug
```

The app builds and its List experience works without a map key.

## Optional Google Maps setup

1. Create a Google Cloud project and enable **Maps SDK for Android**.
2. Create an API key and restrict it to Android application `com.besklar.grounded` and your signing certificate.
3. Add the following to the untracked `local.properties` file:

```properties
MAPS_API_KEY=your_key_here
```

Never commit `local.properties`, credentials, signing files, or usable API keys. The checked-in `DEFAULT_API_KEY` value is an inert placeholder. Without a configured key, Grounded provides an actionable map placeholder and leaves the earthquake list available.

## Architecture

Grounded is a single-activity Compose application using unidirectional data flow. Room is the durable source of truth, repositories coordinate local and remote data, ViewModels expose immutable UI state, and composables render that state and send user actions upward.

Detailed product behavior, privacy, testing, tradeoffs, and attribution will be documented as their features land.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
