# Implementation roadmap

The project is implemented in small stages. Each stage must be useful on its own, and every proposed dependency or abstraction must justify its maintenance cost.

## Stage 1 — Android foundation

- [x] Confirm tablet-first product scope.
- [x] Document architecture and domain boundaries.
- [x] Create a native Android project with Kotlin and Jetpack Compose.
- [x] Add the Gradle Wrapper and pinned build versions.
- [x] Add a minimal startup screen.
- [x] Add a small CI build workflow.

Acceptance: project files and build configuration are in the repository. Full Android Gradle build confirmation is still pending.

## Stage 2 — tier-list engine

- [x] Define the smallest domain model needed for anime, tiers, and placements.
- [x] Implement create, add, move, reorder, remove, and tier configuration operations.
- [x] Prevent duplicate anime entries in the same list.
- [x] Add undo/redo.
- [x] Add six plain Kotlin/JUnit test cases.
- [x] Run a local Kotlin compiler validation for defaults, add/move/reorder, tier editing/removal, and undo/redo.

Acceptance: pure Kotlin operations work independently of Android UI and database. The JUnit task is also wired into GitHub Actions; CI result is pending verification.

## Stage 3 — first usable editor

### 3A — basic board interaction

- [x] Render default S/A/B/C/D/E tiers.
- [x] Add anime by title to the unranked area.
- [x] Move an entry between tiers or back to unranked using its menu.
- [x] Reorder entries inside a tier using menu actions.
- [x] Remove an entry from the current board.
- [x] Undo/redo editor operations.

### 3B — board editing and interaction polish

- [ ] Edit tier labels and colors.
- [ ] Change tier order.
- [ ] Add drag-and-drop between tiers and within a tier.
- [ ] Improve tablet landscape/portrait and smaller-screen behavior.
- [ ] Replace text-only entries with locally imported poster artwork in the library stage.

Acceptance: a complete in-memory tier list can be created, edited, and presented clearly. Saving and poster artwork are intentionally separate stages.

## Stage 4 — library and local artwork

- [ ] Add a reusable anime library.
- [ ] Import poster files with Android's system photo/file picker.
- [ ] Store imported artwork in app-managed storage.
- [ ] Add search and duplicate detection.
- [ ] Make library and artwork available offline.

Acceptance: a tier list can be built using locally stored anime and posters without internet.

## Stage 5 — persistence and backups

- [ ] Select storage based on the finished domain model; avoid persistence abstractions before then.
- [ ] Save lists and library through app restarts.
- [ ] Add safe migrations and recovery behavior.
- [ ] Add versioned export/import and backup validation.

Acceptance: edits survive process death, restart, and a validated restore.

## Stage 6 — projector mode

- [ ] Detect connected external displays.
- [ ] Use a separate fullscreen Presentation where supported.
- [ ] Keep the projected board synchronized with tablet edits.
- [ ] Handle rotation, disconnect/reconnect, and different resolutions.
- [ ] Explain or gracefully fall back to mirroring when separate output is unavailable.

Acceptance: the board is readable and stable on the user's actual tablet/projector setup.

## Stage 7 — convention participation

- [ ] Add presenter and public self-service modes.
- [ ] Add moderation and reliable undo.
- [ ] Design optional phone suggestions and voting.
- [ ] Add a local network service only if the phone workflow requires it.

Acceptance: attendees can contribute, but cannot silently alter the official ranking.

## Later — desktop

Reconsider a desktop target only when there is a real use case. Reuse tested domain code where practical; do not compromise the Android MVP for a hypothetical port.
