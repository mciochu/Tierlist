# Implementation roadmap

Stages are deliberately small. Prefer platform APIs and plain Kotlin before adding dependencies, modules, services, or abstractions.

## Stage 1 — Android foundation

- [x] Native Kotlin + Jetpack Compose Android application.
- [x] One application module and Gradle Wrapper.
- [x] Minimal build workflow with unit tests and debug APK task.

Full Gradle CI result remains to be verified.

## Stage 2 — tier-list engine

- [x] Immutable tier, entry, and board model.
- [x] Add, move, reorder, and remove anime entries.
- [x] Add, edit, reorder, and remove tiers.
- [x] Undo/redo snapshots.
- [x] Six JUnit tests and local Kotlin logic validation.

## Stage 3 — editor

- [x] Default S/A/B/C/D/E rows and unranked area.
- [x] Add anime by title.
- [x] Move between rows and reorder with menu actions.
- [x] Remove entries and undo/redo.
- [x] Edit row labels and colors.
- [x] Add and remove rows; change row order.
- [ ] Direct drag-and-drop. Kept out of this pass to avoid a fragile gesture implementation; menu-based moves and reorder are available.
- [ ] Real-device layout verification in tablet portrait and landscape.

## Stage 4 — local artwork and library foundation

- [x] Import poster artwork using Android's system document picker.
- [x] Copy selected artwork into app-private storage for offline use.
- [x] Show poster thumbnails on board entries.
- [x] Keep anime title and artwork metadata locally.
- [ ] Separate reusable library screen and search/selection flow.
- [ ] Duplicate detection across multiple tier lists.

No remote catalogue client is included yet. Local import is the reliable offline-first path and avoids API keys, rate limits, and network dependencies.

## Stage 5 — persistence

- [x] Automatic save of board, rows, placements, titles, and poster paths.
- [x] Restore board after process restart.
- [x] Version field in the local JSON document.
- [ ] User-facing export/import and backup validation.
- [ ] Recovery UI for malformed or older saved data.

The first version uses Android SharedPreferences + org.json and app-private poster files. Room/SQLDelight is not justified for a single local board and a small dataset yet; reconsider only when multiple lists and a larger searchable library arrive.

## Stage 6 — projector

- [x] Detect presentation-capable external displays.
- [x] Open a separate read-only Presentation when one is connected.
- [x] Keep projection synchronized with editor changes.
- [x] Render tier labels, colors, poster art, and titles.
- [ ] Test HDMI/USB-C and wireless display on the actual tablet.
- [ ] Tune projection for 16:9 and unusual projector resolutions.
- [ ] Add a visible mirror-mode fallback/help message when no presentation display is exposed by Android.

A tablet may mirror instead of exposing a separate display. Separate control and projection screens depend on the tablet, adapter, and casting path.

## Stage 7 — convention workflow

- [ ] Presenter lock and public self-service mode.
- [ ] Safe session reset/new ranking flow.
- [ ] Suggestions and voting from attendee phones.
- [ ] Local network service only if phone participation is implemented.

Phone participation is optional and must not become a requirement for normal offline use.

## Later — desktop

Deferred until Android has been used at a real event. Reuse the plain Kotlin domain model where practical.
