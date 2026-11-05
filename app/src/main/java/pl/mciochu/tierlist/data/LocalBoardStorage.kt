package pl.mciochu.tierlist.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pl.mciochu.tierlist.domain.Tier
import pl.mciochu.tierlist.domain.TierEntry
import pl.mciochu.tierlist.domain.TierList
import pl.mciochu.tierlist.domain.PositionChange

data class AnimeAsset(
    val title: String,
    val posterPath: String? = null
)

data class SavedEditor(
    val board: TierList,
    val anime: Map<String, AnimeAsset>,
    val nextAnimeId: Int,
    val positionHistory: List<PositionChange> = emptyList()
)

class LocalBoardStorage(context: Context) {
    private val preferences = context.getSharedPreferences("tier_board", Context.MODE_PRIVATE)

    fun load(): SavedEditor? {
        val raw = preferences.getString("editor_state", null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val tiersJson = root.getJSONArray("tiers")
            val entriesJson = root.getJSONArray("entries")
            val animeJson = root.getJSONObject("anime")
            val historyJson = root.optJSONArray("positionHistory") ?: JSONArray()

            val tiers = buildList {
                for (index in 0 until tiersJson.length()) {
                    val item = tiersJson.getJSONObject(index)
                    add(
                        Tier(
                            id = item.getString("id"),
                            label = item.getString("label"),
                            colorArgb = item.getLong("color"),
                            position = item.getInt("position")
                        )
                    )
                }
            }
            val entries = buildList {
                for (index in 0 until entriesJson.length()) {
                    val item = entriesJson.getJSONObject(index)
                    add(
                        TierEntry(
                            animeId = item.getString("animeId"),
                            tierId = item.optString("tierId").takeIf { it.isNotEmpty() },
                            position = item.getInt("position")
                        )
                    )
                }
            }
            val assets = buildMap {
                val keys = animeJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = animeJson.getJSONObject(key)
                    put(
                        key,
                        AnimeAsset(
                            title = item.getString("title"),
                            posterPath = item.optString("posterPath").takeIf { it.isNotEmpty() }
                        )
                    )
                }
            }
            val positionHistory = buildList {
                for (index in 0 until historyJson.length()) {
                    val item = historyJson.getJSONObject(index)
                    add(
                        PositionChange(
                            animeId = item.getString("animeId"),
                            fromTierId = if (item.isNull("fromTierId")) null else item.getString("fromTierId"),
                            fromTierLabel = if (item.isNull("fromTierLabel")) null else item.getString("fromTierLabel"),
                            fromPosition = if (item.isNull("fromPosition")) null else item.optInt("fromPosition"),
                            toTierId = if (item.isNull("toTierId")) null else item.getString("toTierId"),
                            toTierLabel = if (item.isNull("toTierLabel")) null else item.getString("toTierLabel"),
                            toPosition = if (item.isNull("toPosition")) null else item.optInt("toPosition"),
                            operation = item.optString("operation", "MOVE"),
                            timestamp = item.optLong("timestamp")
                        )
                    )
                }
            }
            SavedEditor(
                board = TierList(
                    id = root.getString("id"),
                    name = root.getString("name"),
                    tiers = tiers,
                    entries = entries
                ),
                anime = assets,
                nextAnimeId = root.optInt("nextAnimeId", assets.size + 1),
                positionHistory = positionHistory
            )
        }.getOrNull()
    }

    fun save(state: SavedEditor) {
        val root = JSONObject()
            .put("version", 2)
            .put("id", state.board.id)
            .put("name", state.board.name)
            .put("nextAnimeId", state.nextAnimeId)

        val tiers = JSONArray()
        state.board.tiers.forEach {
            tiers.put(
                JSONObject()
                    .put("id", it.id)
                    .put("label", it.label)
                    .put("color", it.colorArgb)
                    .put("position", it.position)
            )
        }
        val entries = JSONArray()
        state.board.entries.forEach {
            entries.put(
                JSONObject()
                    .put("animeId", it.animeId)
                    .put("tierId", it.tierId ?: "")
                    .put("position", it.position)
            )
        }
        val anime = JSONObject()
        state.anime.forEach { (id, asset) ->
            anime.put(
                id,
                JSONObject()
                    .put("title", asset.title)
                    .put("posterPath", asset.posterPath ?: "")
            )
        }
        val history = JSONArray()
        state.positionHistory.forEach { change ->
            history.put(
                JSONObject()
                    .put("animeId", change.animeId)
                    .put("fromTierId", change.fromTierId ?: JSONObject.NULL)
                    .put("fromTierLabel", change.fromTierLabel ?: JSONObject.NULL)
                    .put("fromPosition", change.fromPosition ?: JSONObject.NULL)
                    .put("toTierId", change.toTierId ?: JSONObject.NULL)
                    .put("toTierLabel", change.toTierLabel ?: JSONObject.NULL)
                    .put("toPosition", change.toPosition ?: JSONObject.NULL)
                    .put("operation", change.operation)
                    .put("timestamp", change.timestamp)
            )
        }
        root.put("tiers", tiers).put("entries", entries).put("anime", anime)
            .put("positionHistory", history)
        preferences.edit().putString("editor_state", root.toString()).apply()
    }
}
