# Data model

This document defines the initial conceptual model. Concrete SQL types and indexes will be finalized alongside the first database migration.

## Entities

### Anime
- `id`: stable UUID.
- `primaryTitle`: required display title.
- `normalizedTitle`: search/deduplication helper.
- `aliases`: optional alternate titles.
- `externalIds`: optional provider-specific identifiers (for example AniList or MAL).
- `createdAt`, `updatedAt`.

### Artwork
- `id`: stable UUID.
- `animeId`: owning Anime.
- `localPath`: app-managed file path.
- `mimeType`, `width`, `height`: optional metadata.
- `sourceUrl`, `attribution`: optional provenance.
- `isPreferred`: whether this is the default poster.

### TierBoard
- `id`: stable UUID.
- `name`: board title.
- `createdAt`, `updatedAt`.

### Tier
- `id`: stable UUID.
- `boardId`: owning board.
- `label`: e.g. S, A, B, C, D, E.
- `color`: stored color representation.
- `position`: stable ordered position.

### TierEntry
- `id`: stable UUID.
- `boardId`, `animeId`.
- `tierId`: nullable while unranked.
- `position`: order within the tier.
- Unique constraint: one anime entry per board unless a later product decision explicitly permits duplicates.

### Session
- `id`: stable UUID.
- `boardId`: board being presented.
- `title`, `createdAt`, `updatedAt`.
- `status`: draft, active, or archived.

### ChangeRecord
- `id`: stable UUID.
- `sessionId`.
- `operationType`.
- `payload`: versioned reversible operation data.
- `createdAt`.

## Important relationships

- One Anime may have many Artwork records.
- One Anime may appear on many boards.
- Each board owns its own tiers and entries.
- A session references the board being used; it does not duplicate the library.
- Removing an entry never deletes the Anime or its Artwork.
- Deleting artwork requires an explicit library-level action and a reference check.

## Ordering

Tier order and entry order are explicit persisted values. Reordering must be deterministic. The implementation may use integer positions initially and normalize them transactionally after a reorder. Position updates for a move between tiers must happen in one database transaction.

## Backup format

The external backup format must be versioned independently from the SQLite schema. It must include library records, board definitions, entries, sessions, and artwork files or stable asset references. Import validates version, IDs, relationships, file sizes, and image types before applying changes.
