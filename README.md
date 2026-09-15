# Grounded

Grounded is a calm, location-aware Android companion for understanding recent earthquake activity. It presents official U.S. Geological Survey data as an interactive map and readable list, keeps the last successful result available offline, and can add approximate distance context without storing location history.

Grounded is informational. It is not an earthquake early-warning or emergency-safety system.

![Grounded earthquake list on Android](docs/grounded-home.png)

## Features

- Recent worldwide earthquake activity from the official USGS past-day GeoJSON feed
- Scan-friendly list and interactive Google map backed by one shared dataset
- Full event details, official USGS links, and Android sharing
- Offline-first Room cache with explicit freshness and failed-refresh states
- Optional one-shot approximate location for local distance and compass direction
- System light/dark theme, dynamic color, scalable text, and screen-reader semantics
- No account, custom backend, analytics, continuous tracking, or stored location history

## Requirements

- Android Studio 2026.1 or compatible
- JDK 17+
- Android SDK 36
- Android 12 (API 31) or newer device/emulator

## Build and run

Clone the repository, open it in Android Studio, allow Gradle sync to finish, and run the `app` configuration. From a terminal:

```shell
./gradlew assembleDebug
```

The APK is written under `app/build/outputs/apk/debug/`. The application and List experience build and run without a map key.

## Optional Google Maps setup

1. Create a Google Cloud project with billing configured.
2. Enable **Maps SDK for Android**.
3. Create an API key and restrict it to Android application `com.besklar.grounded` and the relevant signing-certificate fingerprint.
4. Add the value to the untracked root `local.properties` file:

```properties
MAPS_API_KEY=your_key_here
```

The checked-in `local.defaults.properties` contains only the inert `DEFAULT_API_KEY` placeholder. Never commit `local.properties`, credentials, service-account files, signing files, or usable API keys. If the key is absent, Grounded shows setup guidance in Map while List remains usable.

## Architecture

Grounded is a single-activity, single-module application using MVVM and unidirectional data flow:

```text
USGS → Retrofit → Repository → Room → Flow → ViewModel → Compose
```

- **Remote layer:** private serialization DTOs and a mapper convert partial USGS input into a stable domain model. A malformed event can be discarded without losing other usable events.
- **Data layer:** Room is the durable source of truth. Refreshes atomically replace events and metadata only after a valid response; failed refreshes retain the last successful snapshot.
- **UI layer:** screen-level ViewModels expose immutable `StateFlow`. Compose renders state and sends user actions upward. Navigation passes stable event IDs rather than serialized screen objects.
- **Location:** a singleton in-memory repository holds at most one approximate fix. Pure functions calculate great-circle distance and compass direction locally.

The code intentionally remains one Gradle module. Package boundaries provide separation without paying the build and maintenance cost of premature modularization. Hilt is used only at system boundaries; small product rules remain ordinary testable Kotlin.

## Product decisions

- The default dataset is the USGS past 24 hours, ordered newest first.
- A nearby significant event is magnitude 4.5+ within 805 km / 500 miles.
- Unknown values remain unknown; Grounded never turns missing magnitude into `0.0`.
- Invalid coordinates exclude an event from Map but never from List.
- Saved data becomes visibly stale after 30 minutes from the last successful retrieval.
- New IDs—not revisions of existing IDs—receive a temporary 30-second treatment.
- Map interaction never implies physical wave propagation or continuous monitoring.

## Offline and failure behavior

On launch, cached records appear as soon as Room emits them while an unobtrusive refresh begins. A successful response replaces the snapshot in one database transaction. A failed request never clears good data, and the UI identifies it as saved data with its real age. A valid empty response is different from a failure and intentionally clears the represented feed.

Offline map tiles are not included; the list and cached event details remain the reliable offline surfaces.

## Location and privacy

Grounded does not display the Android permission dialog on first launch. The user first chooses “See earthquakes relative to you” and receives an explanation. Only `ACCESS_COARSE_LOCATION` is requested. No precise or background permission is requested, location is not uploaded, and coordinates are not stored in Room. A last-known location is accepted only when no older than 30 minutes.

Denial leaves all non-relative features working. A permanently denied permission is followed by a user-initiated path to Android application settings.

## Testing

```shell
./gradlew spotlessCheck lintDebug testDebugUnitTest assembleDebug
./gradlew pixel2Api31DebugAndroidTest
```

JVM tests cover USGS normalization, malformed/duplicate input, cache retention, empty success, new/revised event detection, summaries, fallback formatting, safe links/share text, map transformation, and distance/direction calculations. Compose instrumentation tests cover stable loading and the principal list-selection flow. CI runs the same static, unit, build, and API 31 managed-device checks.

Manual release checks should cover:

- First launch online/offline and cached launch offline
- Approximate location granted, denied, permanently denied, services disabled, and unavailable
- Missing/present Maps configuration
- Light/dark themes, large font scale, landscape, and TalkBack
- Refresh with changes, no changes, cached failure, and no-cache failure
- Detail navigation, external USGS link, Android share, rotation, and process recreation

## Known limitations and next steps

- Google Maps requires the developer’s own configured API key and network connectivity for tiles.
- Filters, home-screen widgets, watched-area notifications, background polling, and offline map tiles are not implemented.
- A production alerting feature would require explicit semantics and likely reliable backend push; this app makes no real-time-warning claim.
- Marker clustering can be added if real representative datasets show a measurable usability or performance need.

## Attribution

Earthquake information is provided by the [U.S. Geological Survey Earthquake Hazards Program](https://earthquake.usgs.gov/earthquakes/feed/). Map rendering uses Google Maps Platform when locally configured. Data may be revised by its source.

## Development assistance

AI-assisted development was used for implementation support, test generation, and documentation. Architecture, product behavior, security decisions, and final verification remain subject to human review. No private credentials are intentionally included in source control.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
