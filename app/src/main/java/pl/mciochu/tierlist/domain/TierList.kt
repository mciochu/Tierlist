package pl.mciochu.tierlist.domain

data class Tier(
    val id: String,
    val label: String,
    val colorArgb: Long,
    val position: Int
)

data class TierEntry(
    val animeId: String,
    val tierId: String?,
    val position: Int
)

data class TierList(
    val id: String,
    val name: String,
    val tiers: List<Tier>,
    val entries: List<TierEntry>
) {
    companion object {
        fun createDefault(id: String, name: String): TierList {
            require(id.isNotBlank()) { "Tier-list ID cannot be blank." }
            require(name.isNotBlank()) { "Tier-list name cannot be blank." }

            val defaults = listOf(
                Triple("s", "S", 0xFFE45B5BL),
                Triple("a", "A", 0xFFFF9848L),
                Triple("b", "B", 0xFFE7CE56L),
                Triple("c", "C", 0xFF75B86BL),
                Triple("d", "D", 0xFF5DA5CFL),
                Triple("e", "E", 0xFF9B80C6L)
            )

            return TierList(
                id = id,
                name = name.trim(),
                tiers = defaults.mapIndexed { index, item ->
                    Tier(
                        id = item.first,
                        label = item.second,
                        colorArgb = item.third,
                        position = index
                    )
                },
                entries = emptyList()
            )
        }
    }
}

/**
 * Pure, immutable tier-list operations. IDs are supplied by the caller so
 * this layer stays deterministic and does not need an ID-generation library.
 */
object TierListEditor {
    fun addAnime(list: TierList, animeId: String, tierId: String? = null): TierList {
        require(animeId.isNotBlank()) { "Anime ID cannot be blank." }
        require(list.entries.none { it.animeId == animeId }) {
            "Anime is already present in this tier list."
        }
        requireTierExists(list, tierId)

        val position = list.entries.count { it.tierId == tierId }
        return list.copy(
            entries = list.entries + TierEntry(animeId, tierId, position)
        )
    }

    fun moveAnime(
        list: TierList,
        animeId: String,
        targetTierId: String?,
        targetPosition: Int? = null
    ): TierList {
        requireTierExists(list, targetTierId)
        val moving = list.entries.singleOrNull { it.animeId == animeId }
            ?: throw IllegalArgumentException("Anime is not present in this tier list.")

        val remaining = list.entries.filterNot { it.animeId == animeId }
        val targetEntries = remaining
            .filter { it.tierId == targetTierId }
            .sortedBy { it.position }
            .toMutableList()

        val insertAt = (targetPosition ?: targetEntries.size)
            .coerceIn(0, targetEntries.size)
        targetEntries.add(insertAt, moving.copy(tierId = targetTierId))

        val unaffected = remaining.filter { it.tierId != targetTierId }
        val positionedTarget = targetEntries.mapIndexed { index, entry ->
            entry.copy(position = index)
        }

        return list.copy(entries = normalizeEntries(unaffected + positionedTarget))
    }

    fun removeAnime(list: TierList, animeId: String): TierList {
        require(list.entries.any { it.animeId == animeId }) {
            "Anime is not present in this tier list."
        }
        return list.copy(
            entries = normalizeEntries(list.entries.filterNot { it.animeId == animeId })
        )
    }

    fun addTier(
        list: TierList,
        tierId: String,
        label: String,
        colorArgb: Long,
        position: Int = list.tiers.size
    ): TierList {
        require(tierId.isNotBlank()) { "Tier ID cannot be blank." }
        require(label.isNotBlank()) { "Tier label cannot be blank." }
        require(list.tiers.none { it.id == tierId }) { "Tier ID already exists." }
        require(list.tiers.none { it.label.equals(label.trim(), ignoreCase = true) }) {
            "Tier label already exists."
        }

        val tiers = list.tiers.sortedBy { it.position }.toMutableList()
        val insertAt = position.coerceIn(0, tiers.size)
        tiers.add(insertAt, Tier(tierId, label.trim(), colorArgb, insertAt))
        return list.copy(
            tiers = tiers.mapIndexed { index, tier -> tier.copy(position = index) }
        )
    }

    fun updateTier(
        list: TierList,
        tierId: String,
        label: String? = null,
        colorArgb: Long? = null
    ): TierList {
        val current = list.tiers.singleOrNull { it.id == tierId }
            ?: throw IllegalArgumentException("Tier does not exist.")
        if (label != null) {
            require(label.isNotBlank()) { "Tier label cannot be blank." }
            require(list.tiers.none {
                it.id != tierId && it.label.equals(label.trim(), ignoreCase = true)
            }) { "Tier label already exists." }
        }

        return list.copy(
            tiers = list.tiers.map {
                if (it.id == tierId) {
                    it.copy(
                        label = label?.trim() ?: current.label,
                        colorArgb = colorArgb ?: current.colorArgb
                    )
                } else {
                    it
                }
            }
        )
    }

    fun moveTier(list: TierList, tierId: String, targetPosition: Int): TierList {
        val tiers = list.tiers.sortedBy { it.position }.toMutableList()
        val currentIndex = tiers.indexOfFirst { it.id == tierId }
        require(currentIndex >= 0) { "Tier does not exist." }

        val moving = tiers.removeAt(currentIndex)
        val insertAt = targetPosition.coerceIn(0, tiers.size)
        tiers.add(insertAt, moving)
        return list.copy(
            tiers = tiers.mapIndexed { index, tier -> tier.copy(position = index) }
        )
    }

    fun removeTier(list: TierList, tierId: String): TierList {
        require(list.tiers.any { it.id == tierId }) { "Tier does not exist." }
        require(list.tiers.size > 1) { "A tier list must keep at least one tier." }

        val tiers = normalizeTiers(list.tiers.filterNot { it.id == tierId })
        val entries = list.entries.map {
            if (it.tierId == tierId) it.copy(tierId = null, position = 0) else it
        }
        return list.copy(tiers = tiers, entries = normalizeEntries(entries))
    }

    private fun requireTierExists(list: TierList, tierId: String?) {
        require(tierId == null || list.tiers.any { it.id == tierId }) {
            "Target tier does not exist."
        }
    }

    private fun normalizeTiers(tiers: List<Tier>): List<Tier> =
        tiers.sortedBy { it.position }.mapIndexed { index, tier ->
            tier.copy(position = index)
        }

    private fun normalizeEntries(entries: List<TierEntry>): List<TierEntry> =
        entries.groupBy { it.tierId }.values.flatMap { group ->
            group.sortedBy { it.position }.mapIndexed { index, entry ->
                entry.copy(position = index)
            }
        }
}

/**
 * Snapshot-based undo/redo. The model is immutable, so snapshots reuse
 * unchanged lists instead of copying the whole board for every edit.
 */
data class TierListHistory private constructor(
    val current: TierList,
    private val undoStack: List<TierList>,
    private val redoStack: List<TierList>
) {
    constructor(initial: TierList) : this(initial, emptyList(), emptyList())

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun apply(next: TierList): TierListHistory =
        if (next == current) this
        else TierListHistory(next, undoStack + current, emptyList())

    fun undo(): TierListHistory {
        val previous = undoStack.lastOrNull() ?: return this
        return TierListHistory(
            current = previous,
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + current
        )
    }

    fun redo(): TierListHistory {
        val next = redoStack.lastOrNull() ?: return this
        return TierListHistory(
            current = next,
            undoStack = undoStack + current,
            redoStack = redoStack.dropLast(1)
        )
    }
}
