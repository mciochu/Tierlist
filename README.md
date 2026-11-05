# Anime Tier Board

Offline-first Android application for creating and presenting anime tier lists at conventions.

The Android tablet is the primary and complete device: it stores the anime library and rankings, provides the editor, and can send a presentation to a projector when the tablet supports an external display. No laptop, Raspberry Pi, account, or cloud service is required.

## Current status

**Stage 3 — first interactive editor in progress.** The native Android app, pure Kotlin tier-list engine, undo/redo, and first board screen are in the repository. The current editor is in-memory; library artwork and persistence are later stages.

## Product principles

- Android tablet first.
- Work offline; internet is optional for future catalogue lookups.
- One reusable anime library; placements belong to individual tier lists.
- Presenter remains in control of the official board.
- No accounts or cloud dependency for normal use.
- Preserve rankings and sessions; support explicit backup and restore.
- Add no module, library, service, or abstraction until it solves a real need.

## Planned capabilities

1. Create tier lists and configure their rows, labels, order, and colors.
2. Keep a reusable anime library and attach local poster artwork.
3. Add, move, reorder, and remove posters.
4. Support undo/redo and automatic saving.
5. Present a clean fullscreen board on a compatible external display.
6. Later, optionally accept suggestions and votes from attendee phones.

## Technology

- Kotlin with Android Gradle Plugin's built-in Kotlin support.
- Jetpack Compose and Material 3 for the interface.
- One Android application module; no premature multi-module structure.
- Gradle Wrapper for reproducible builds.
- Persistence, image handling, and projector integration will be added only in the stage where they are needed.

A desktop build and Kotlin Multiplatform are deliberately deferred. The first product must work well on one Android tablet; adding more targets now would increase setup and maintenance without improving the first usable version.

## Build

Open the repository in Android Studio and let Gradle sync.

Requirements:
- JDK 17
- Android SDK Platform 37
- Internet access for the first Gradle/dependency download

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Data model](docs/DATA_MODEL.md)

## License

License decision pending.
