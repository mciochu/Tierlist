package pl.mciochu.tierlist.domain

/**
 * One durable record for an anime whose tier or order changed.
 * Positions are one-based for display; null means the anime was not on the board.
 */
data class PositionChange(
    val animeId: String,
    val fromTierId: String?,
    val fromTierLabel: String?,
    val fromPosition: Int?,
    val toTierId: String?,
    val toTierLabel: String?,
    val toPosition: Int?,
    val operation: String,
    val timestamp: Long
)

/**
 * Creates one event for every anime whose tier or one-based position changed.
 * This includes neighboring anime shifted by an insertion or reorder.
 */
fun buildPositionChanges(
    before: TierList,
    after: TierList,
    operation: String,
    timestamp: Long
): List<PositionChange> {
    val previousEntries = before.entries.associateBy { it.animeId }
    val nextEntries = after.entries.associateBy { it.animeId }
    val previousTiers = before.tiers.associateBy { it.id }
    val nextTiers = after.tiers.associateBy { it.id }

    return (previousEntries.keys + nextEntries.keys).mapNotNull { animeId ->
        val old = previousEntries[animeId]
        val new = nextEntries[animeId]
        if (old?.tierId == new?.tierId && old?.position == new?.position) {
            null
        } else {
            PositionChange(
                animeId = animeId,
                fromTierId = old?.tierId,
                fromTierLabel = old?.tierId?.let { previousTiers[it]?.label },
                fromPosition = old?.position?.plus(1),
                toTierId = new?.tierId,
                toTierLabel = new?.tierId?.let { nextTiers[it]?.label },
                toPosition = new?.position?.plus(1),
                operation = operation,
                timestamp = timestamp
            )
        }
    }
}
