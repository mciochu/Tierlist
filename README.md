# Anime Tier Board

Offline-first Android application for building and presenting anime tier lists at conventions.

The Android tablet is the primary device: it stores the anime library and ranking sessions, provides the editor, and drives a projector when the tablet supports an external display. A projector is an output only; no laptop, Raspberry Pi, or separate controller is required.

## Project status

**Phase 0 — foundation and architecture.** The repository is being initialized. The first usable milestone is a local tier-list editor with persistent data and undo/redo. Projector output and attendee participation follow after the core workflow is stable.

## Product principles

- Android tablet first; desktop is a future target, not a requirement.
- Offline-first: an event must remain usable without internet access.
- One source of truth for anime metadata and artwork, with separate placements per ranking.
- Presenter remains in control; public suggestions and voting never silently change the board.
- No accounts or cloud service required for the basic experience.
- Preserve sessions through app restarts and provide import/export backups.
- Avoid unnecessary Gradle modules and premature networking infrastructure.

## Planned capabilities

1. Create and configure tier boards (labels, order, colors).
2. Maintain a reusable anime library and attach local poster artwork.
3. Add, move, reorder, and remove posters with undo/redo.
4. Autosave sessions and restore them after restart.
5. Present a clean, fullscreen board on a supported external display; mirror the tablet when separate display output is unavailable.
6. Later: optional QR-based suggestions and voting from attendee phones.

## Technical direction

- Kotlin + Compose Multiplatform.
- Android application as the first shipping target.
- Shared Kotlin domain and presentation code, with Android-specific display integration.
- SQLite persistence; database access behind repositories.
- Local image storage/cache and explicit import/export.
- Desktop target kept as a future-compatible direction; it must not complicate the Android MVP.

## Development

Open the project in a current Android Studio with Kotlin Multiplatform support. The Gradle wrapper will be added with the first verified build environment. See [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## License

License decision pending.
