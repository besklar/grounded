# Architecture

Grounded uses a deliberately small MVVM architecture with unidirectional data flow:

```text
USGS API → Retrofit mapper → Repository → Room → Flow → ViewModel → Compose
                                                           ↑          │
                                                           └─ actions ┘
```

## Why this shape

Room is the source of truth. The UI never renders Retrofit objects and a network response is never held as a second competing copy of product data. A refresh first validates and normalizes the response, then replaces events and snapshot metadata in one database transaction. Room emits the new snapshot as a `Flow`; the ViewModel turns that stream into immutable `HomeUiState`; Compose redraws only the parts whose inputs changed.

This is MVVM because each screen has a ViewModel that owns screen state and coordinates application services. It is also unidirectional data flow because values move down into composables while user actions move up through callbacks. The labels matter less than the constraint: there is one understandable route by which state changes.

## Responsibilities

- `data/remote` owns the external JSON contract. Its DTOs are private implementation details and its mapper rejects unusable records without inventing missing values.
- `data/local` owns Room entities, snapshot metadata, DAOs, and atomic replacement.
- `data/repository` coordinates remote refresh and local persistence. It reports typed outcomes while Room remains the observable data source.
- `model` contains stable application models that do not depend on Retrofit, Room, or Compose.
- `location` owns opt-in, memory-only approximate location and local distance calculations.
- `ui` owns ViewModels, immutable screen state, formatting, navigation, and composables.

Hilt wires long-lived system boundaries such as the database, HTTP client, repository, and location provider. It is not used to disguise simple calculations behind interfaces. Pure logic stays as ordinary Kotlin because direct code is easier to test and explain.

## State ownership

The ViewModel owns data that affects the screen as a product: cached content, refresh status, selected mode/event, freshness, location context, and temporary new-event IDs. Compose owns short-lived visual state such as whether a sheet is open. Saveable state is used only where recreating an Activity should preserve the user’s place.

Refreshes are serialized to one job. Cached content remains visible during refresh, failures do not destroy it, and the ViewModel waits for Room’s first snapshot emission before deciding whether refreshed IDs are genuinely new. That last detail prevents startup scheduling from changing product behavior.

The repository caches one seven-day feed rather than separate results per filter. A pure selector applies the chosen time range, magnitude threshold, and ordering in memory; that exact list is passed to the summary, map, list, and empty-state decision. This is analogous to a memoized Redux selector: the durable store remains unchanged while the UI derives a focused view. Nearest ordering uses local distance calculation and falls back explicitly to recent ordering when location is unavailable.

## Decisions intentionally deferred

- No multi-module build: one application does not yet justify dependency graphs and slower project navigation.
- No generic use-case layer: it would mostly rename repository calls without adding policy.
- No MVI framework: the app already has explicit immutable state and actions without another abstraction.
- No background polling, notifications, widget, backend, analytics, or persisted location: each adds lifecycle, privacy, and reliability obligations outside the browsing experience.
- No offline map tiles: cached list and details are the dependable offline surfaces.
