# Technical architecture

## 1. Product shape

Anime Tier Board is a single-device-first application. An Android tablet owns the library, ranking state, editing operations, persistence, and presentation. A projector is an optional display endpoint.

The primary workflow must work with:
- one Android tablet;
- no network connection;
- locally available posters;
- optional HDMI or supported wireless display connection.

Attendee phones are an optional later extension. They are not needed to operate the application at a convention.

## 2. Runtime boundaries

### Shared application layer
- Domain entities and validation.
- Tier-list operations and ordering rules.
- Session lifecycle and autosave policy.
- Library search/filtering.
- UI state and reusable Compose UI.
- Import/export format and migration logic.

### Android platform layer
- Activity lifecycle and app entry point.
- File picker and image import.
- Local database and filesystem integration.
- External display discovery and lifecycle.
- A dedicated projector presentation when supported by the device.
- Fallback to ordinary screen mirroring/casting when a separate presentation display is unavailable.

Important: USB-C does not guarantee video output. Device capability must be checked on the actual tablet. Wireless casting may mirror rather than expose a separate display. The app must not assume every Android device supports independent projector content.

### Future optional participation layer
A local HTTP/WebSocket service may later allow phones on the same local network or tablet hotspot to submit suggestions and votes. This must be opt-in, bounded to a session, and isolated from the core editor. The presenter approves changes to the official board.

## 3. Suggested code organization

Start with one application module and clear package boundaries. Split into Gradle modules only when build time, ownership, or dependency isolation justifies it.

```
composeApp/
  src/
    commonMain/
      kotlin/.../
        app/
        core/domain/
        core/data/
        core/design/
        feature/home/
        feature/library/
        feature/board/
        feature/sessions/
        feature/settings/
    androidMain/
      kotlin/.../
        platform/android/
    desktopMain/                 # future target; not required for MVP
docs/
```

## 4. Domain model

- **Anime**: stable internal ID, title, optional external catalog IDs, aliases.
- **Artwork**: image ID, owning anime ID, local path, source/credit metadata, preferred flag.
- **TierBoard**: ID, name, ordered tier definitions, presentation settings.
- **Tier**: ID, board ID, label, color token, display order.
- **TierEntry**: ID, board ID, anime ID, tier ID (nullable for unranked), order within tier.
- **Session**: ID, board ID, title, created/updated timestamps, active state.
- **ChangeRecord**: operation ID, session ID, operation type, before/after state, timestamp.

An anime exists once in the library and can appear in multiple boards. Placement and order belong to the board entry, never to the anime record.

## 5. State changes

All board edits go through explicit domain operations:
- add anime to board;
- move entry between tiers;
- reorder entries within a tier;
- remove entry from board (does not delete the library anime);
- configure tier labels/colors/order;
- undo and redo.

Operations validate IDs and ordering constraints before persistence. Undo/redo stores reversible operations, not screenshots of the entire UI. Autosave commits successful operations; UI state must not be treated as the durable source of truth.

## 6. Persistence and files

- SQLite is the durable source of truth for library, boards, tiers, entries, sessions, and change history.
- Artwork bytes are stored as managed local files; database rows store paths and metadata.
- Importing an image copies it into app-managed storage. Removing an anime from a board must never delete its artwork.
- Database schema changes require explicit migrations.
- Export a versioned JSON backup plus referenced artwork assets (or a documented archive format).
- Import validates the full payload before modifying existing data. Prefer restore into a new workspace or explicit merge; never silently overwrite.

## 7. Projection

The projection surface contains only the board and presentation-safe labels. It must not expose library search, editing controls, attendee data, or settings.

The Android implementation should:
1. observe display availability and configuration changes;
2. create a dedicated presentation on a compatible secondary display;
3. keep the board synchronized with the tablet's committed state;
4. release the presentation cleanly when the display disconnects;
5. retain a useful tablet-only experience if no secondary display is available.

The board should be legible at a distance, adapt to landscape resolutions, and avoid scrolling during normal presentation. Dense boards may use a compact poster layout and controlled zoom.

## 8. Reliability and privacy

- No account or cloud dependency for MVP.
- No telemetry requirement.
- No background server in the MVP.
- Autosave after each accepted edit.
- Safe handling of app process death and display disconnects.
- Optional phone participation must not expose local files or arbitrary device access.

## 9. Architectural decisions

- **Android first:** convention use is tablet-centric; other targets must not delay it.
- **Offline first:** internet catalog lookups are convenience features only.
- **Presenter authority:** attendee input is a proposal, not a direct mutation.
- **Single anime library:** board entries reference library entities to prevent duplicate metadata.
- **Simple module structure:** packages first, Gradle modules later.
