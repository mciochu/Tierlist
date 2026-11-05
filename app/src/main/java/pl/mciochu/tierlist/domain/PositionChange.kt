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
