# Implementation roadmap

## Phase 0 — repository foundation
- [x] Confirm repository and product direction.
- [x] Record architecture, scope, and data boundaries.
- [ ] Add buildable Android/Compose project skeleton.
- [ ] Add CI build and unit-test workflow after first local build is verified.

## Phase 1 — tier-list engine (first functional milestone)
- [ ] Define domain entities and stable IDs.
- [ ] Implement tier configuration and ordering rules.
- [ ] Implement add, move, reorder, and remove operations.
- [ ] Implement undo/redo with reversible operation history.
- [ ] Add unit tests for invalid IDs, duplicate entries, ordering, and undo/redo.
- [ ] Create a basic board editor using sample data.

Acceptance: a user can create a board, add sample anime, move posters between tiers, reorder them, undo/redo, and retain the board after restarting the app.

## Phase 2 — library and local artwork
- [ ] Create searchable anime library.
- [ ] Import poster images from Android storage picker.
- [ ] Store artwork in app-managed files.
- [ ] Add duplicate detection using external IDs and normalized titles.
- [ ] Add artwork replacement and multiple artwork variants.
- [ ] Add offline-first behavior and clear empty/error states.

Acceptance: a complete board can be created and edited without internet access.

## Phase 3 — persistence and session management
- [ ] Add SQLite schema and migrations.
- [ ] Persist library, boards, tiers, entries, sessions, and operation history.
- [ ] Add autosave and crash/restart recovery tests.
- [ ] Add versioned export/import and backup validation.

Acceptance: app restart does not lose work; backup can be exported and restored safely.

## Phase 4 — projector presentation
- [ ] Detect compatible external displays.
- [ ] Implement Android Presentation for independent projector content where supported.
- [ ] Add fullscreen board-only layout.
- [ ] Add mirroring/casting fallback guidance.
- [ ] Test connect/disconnect, rotation, resolution changes, and long sessions.

Acceptance: a live board remains readable and synchronized on supported projectors, with a clear fallback on unsupported hardware.

## Phase 5 — convention interaction
- [ ] Add presenter mode and public self-service mode.
- [ ] Add moderation controls, undo, and inactivity/reset safeguards.
- [ ] Design optional phone suggestions and voting.
- [ ] Only then evaluate local hotspot/network server requirements.
- [ ] Keep official board mutations presenter-approved.

Acceptance: attendees can contribute without taking control of the official board.

## Phase 6 — polish and scale
- [ ] Optimize large libraries (500+ artwork items).
- [ ] Add PNG export.
- [ ] Add accessibility and large-screen readability checks.
- [ ] Test portrait/landscape tablet layouts.
- [ ] Test low-storage and interrupted-import cases.

## Later — desktop
A desktop target may reuse shared domain and UI code after Android workflows are proven. It is not a dependency of the convention MVP.
