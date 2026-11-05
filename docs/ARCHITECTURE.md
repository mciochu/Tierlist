# Technical architecture

## 1. Scope

Build an Android-first application for one tablet. The tablet owns the library, tier lists, editing, persistence, and presentation. A projector is an optional output, not a second computer.

The MVP must work offline and without attendee phones.

## 2. Keep the implementation small

Start with one Gradle application module and package-level separation. Do not create Gradle modules for every feature. Add a ViewModel, repository, database, networking layer, or dependency-injection framework only when the feature that needs it is being implemented.

Initial source layout:

```
app/
  src/main/
    AndroidManifest.xml
    java/pl/mciochu/tierlist/
      MainActivity.kt
      # Domain and UI packages are added alongside real features.
docs/
```

Kotlin Multiplatform and a desktop target are deferred. They add build and platform complexity before there is a second product target. Keep domain logic Android-independent where that happens naturally, but do not create an abstraction solely for a hypothetical port.

## 3. UI

- Kotlin + Jetpack Compose.
- Material 3 components where they fit.
- Tablet-first landscape layout, with phone-sized layout still usable for development.
- Keep presentation UI separate from editing controls when projector support is implemented.
- Avoid a custom design system until the UI demonstrates repeated patterns that justify one.

## 4. Domain concepts

- **Anime**: stable ID, main title, optional aliases and external catalogue IDs.
- **Artwork**: stable ID, anime ID, locally managed file path, optional source/attribution.
- **TierList**: stable ID, name, settings.
- **Tier**: stable ID, tier-list ID, label, color, order.
- **TierEntry**: stable ID, tier-list ID, anime ID, tier ID, order in tier.
- **Session**: optional presentation/run referencing a tier list.
- **ChangeRecord**: reversible edit data for undo/redo where needed.

Anime metadata and artwork are library data. Tier membership, tier, and order belong to a specific tier list. Removing a poster from a list must not delete the anime from the library.

## 5. Editing rules

The tier-list engine should be plain Kotlin and have no Android UI dependency:
- add a library anime to a list;
- move an entry between tiers;
- reorder entries;
- remove an entry from a list;
- add, remove, rename, recolor, and reorder tiers;
- undo and redo successful edits.

Operations validate their inputs and produce deterministic results. Unit tests cover ordering, duplicate entries, invalid IDs, moves, and undo/redo.

Do not add a separate event-sourcing framework. A small, explicit undo stack is sufficient until real usage proves otherwise.

## 6. Persistence

Persistence is not part of the initial skeleton. When the editor data model is stable, choose the smallest reliable storage solution for the actual relationships and migration needs. Keep database access out of Compose UI.

Artwork bytes should live in app-managed files; the database should store metadata and paths. Import copies images into app storage. Removing a TierEntry never removes Artwork.

Backup format must be versioned and validated before restoring. Never silently overwrite an existing library or list during import.

## 7. Projection

Projection is a later Android-specific feature:
1. detect external displays;
2. use Android's Presentation API for an independent board when the connected device exposes a compatible secondary display;
3. update the board from the same committed tier-list state;
4. release the presentation cleanly on disconnect;
5. retain normal tablet operation when separate output is unavailable.

USB-C does not guarantee wired video output. Some casting methods mirror the tablet rather than expose an independent display. The application must handle these cases explicitly and should be tested with the actual tablet and projector.

The projected surface contains only the ranking and presentation-safe labels, not editing controls.

## 8. Attendee participation

QR-based suggestions and voting are optional future features. They must not be required for local use. The presenter approves changes to the official ranking. A local network server is not justified until this feature is implemented.

## 9. Platform references

- Android secondary displays: DisplayManager and Presentation APIs.
- Android Jetpack Compose and Material 3.
- Gradle Wrapper for reproducible builds.
