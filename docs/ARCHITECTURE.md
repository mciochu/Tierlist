# Architecture

## Product boundary

The Android tablet is the host, editor, local library, and persistence device. A projector is an optional display output. The application must remain useful offline and must not require a laptop, Raspberry Pi, cloud account, or attendee phone.

## Current implementation

One Android application module keeps the first release small:

- `domain/TierList.kt`: immutable tier-list model, editing operations, and undo/redo.
- `data/LocalBoardStorage.kt`: versioned JSON save/restore through Android SharedPreferences.
- `MainActivity.kt`: Compose editor, row controls, anime entry controls, and local poster import.
- `BoardProjection.kt`: read-only external display using Android Presentation and platform Views.

The editor is the source of truth. The projector receives immutable snapshots and cannot change the board.

## Data ownership

A tier list owns its ordered rows and placements. Each entry refers to an anime ID. The current first release keeps the anime title and optional local poster path in a small local asset map. A later library screen can make that map reusable across multiple tier lists without changing the placement model.

Poster files are copied into app-private storage. The app does not depend on a temporary content URI remaining readable or on network access.

## Persistence choice

The current data volume does not justify Room, SQLDelight, or a database migration framework. A versioned JSON document stored in SharedPreferences is sufficient for the first board. Poster binaries remain separate files in app-private storage.

Before supporting multiple large lists, advanced search, or concurrent writes, revisit the persistence choice. Any migration must preserve existing local boards and poster files.

## Projector output

The activity observes Android's DisplayManager. When Android exposes a presentation display, a separate read-only Presentation renders the current board. If no such display exists, the editor remains available on the tablet and the device's ordinary screen mirroring/casting behavior is left to Android and the hardware.

The app must not promise a separate projection surface on every USB-C tablet or wireless receiver.

## Deferred by design

- Remote anime catalogue/API integration.
- Accounts, cloud sync, and backend.
- Phone voting/local HTTP server.
- Desktop/Kotlin Multiplatform target.
- Dependency injection and additional Gradle modules.

Each item requires a concrete user workflow before implementation.
