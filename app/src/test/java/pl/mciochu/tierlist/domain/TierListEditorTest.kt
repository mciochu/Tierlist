package pl.mciochu.tierlist.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TierListEditorTest {
    private fun newList() = TierList.createDefault("board-1", "Anime")

    @Test
    fun defaultListCreatesSixOrderedTiers() {
        val list = newList()

        assertEquals(listOf("S", "A", "B", "C", "D", "E"), list.tiers.map { it.label })
        assertEquals(listOf(0, 1, 2, 3, 4, 5), list.tiers.map { it.position })
        assertTrue(list.entries.isEmpty())
    }

    @Test
    fun addingAnimeRejectsDuplicatesAndUnknownTiers() {
        val list = TierListEditor.addAnime(newList(), "anime-1", "s")

        assertEquals(1, list.entries.size)
        assertThrows(IllegalArgumentException::class.java) {
            TierListEditor.addAnime(list, "anime-1", "s")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TierListEditor.addAnime(list, "anime-2", "missing-tier")
        }
    }

    @Test
    fun movingAnimeChangesTierAndKeepsPositionsContinuous() {
        var list = newList()
        list = TierListEditor.addAnime(list, "a", "s")
        list = TierListEditor.addAnime(list, "b", "s")
        list = TierListEditor.addAnime(list, "c", "s")
        list = TierListEditor.addAnime(list, "d", "a")

        list = TierListEditor.moveAnime(list, "b", "a", 0)

        assertEquals(listOf("a", "c"), list.entries.filter { it.tierId == "s" }
            .sortedBy { it.position }.map { it.animeId })
        assertEquals(listOf("b", "d"), list.entries.filter { it.tierId == "a" }
            .sortedBy { it.position }.map { it.animeId })
        assertEquals(listOf(0, 1), list.entries.filter { it.tierId == "a" }
            .sortedBy { it.position }.map { it.position })
    }

    @Test
    fun removingTierKeepsItsAnimeUnranked() {
        var list = newList()
        list = TierListEditor.addAnime(list, "anime-1", "s")
        list = TierListEditor.removeTier(list, "s")

        assertFalse(list.tiers.any { it.id == "s" })
        assertEquals(1, list.entries.size)
        assertEquals(null, list.entries.single().tierId)
    }

    @Test
    fun tiersCanBeInsertedMovedAndRenamed() {
        var list = newList()
        list = TierListEditor.addTier(list, "f", "F", 0xFF333333L, 2)

        assertEquals(
            listOf("S", "A", "F", "B", "C", "D", "E"),
            list.tiers.sortedBy { it.position }.map { it.label }
        )

        list = TierListEditor.moveTier(list, "f", 0)
        list = TierListEditor.updateTier(list, "f", label = "Custom", colorArgb = 0xFF112233L)

        assertEquals("Custom", list.tiers.first().label)
        assertEquals(0xFF112233L, list.tiers.first().colorArgb)
        assertEquals(0, list.tiers.first().position)
        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6),
            list.tiers.map { it.position }
        )
    }

    @Test
    fun historySupportsUndoRedoAndClearsRedoAfterNewEdit() {
        val initial = newList()
        val added = TierListEditor.addAnime(initial, "anime-1", "s")
        val afterAdd = TierListHistory(initial).apply(added)

        assertTrue(afterAdd.canUndo)
        val undone = afterAdd.undo()
        assertEquals(0, undone.current.entries.size)
        assertTrue(undone.canRedo)

        val redone = undone.redo()
        assertEquals(1, redone.current.entries.size)
        assertFalse(redone.canRedo)

        val changed = TierListEditor.addAnime(redone.current, "anime-2", "a")
        assertFalse(redone.apply(changed).canRedo)
    }
}
