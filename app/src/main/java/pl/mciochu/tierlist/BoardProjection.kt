package pl.mciochu.tierlist

import android.app.Presentation
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.view.Display
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import pl.mciochu.tierlist.data.SavedEditor
import pl.mciochu.tierlist.domain.TierEntry

/**
 * A separate, read-only projection surface. The tablet remains the controller.
 * Uses platform Views so no additional UI or window-management dependency is needed.
 */
class BoardProjection(
    context: Context,
    display: Display,
    private var state: SavedEditor
) : Presentation(context, display) {

    fun update(next: SavedEditor) {
        state = next
        render()
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
            TextView(context).apply {
                text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER_VERTICAL
            }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(18))
            setBackgroundColor(AndroidColor.rgb(17, 19, 24))
        }
        root.addView(
            text(state.board.name, 28f, AndroidColor.WHITE, true),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
        )

        val scroll = ScrollView(context)
        val tiersColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        state.board.tiers.sortedBy { it.position }.forEach { tier ->
            val entries = state.board.entries.filter { it.tierId == tier.id }
                .sortedBy { it.position }
            tiersColumn.addView(
                createTierRow(
                    tier.label,
                    tier.colorArgb.toInt(),
                    entries,
                    dp(12),
                    dp(104),
                    dp(136),
                    ::text
                )
            )
        }
        val unranked = state.board.entries.filter { it.tierId == null }.sortedBy { it.position }
        if (unranked.isNotEmpty()) {
            tiersColumn.addView(
                createTierRow(
                    "Do umieszczenia",
                    AndroidColor.DKGRAY,
                    unranked,
                    dp(12),
                    dp(104),
                    dp(136),
                    ::text
                )
            )
        }
        scroll.addView(tiersColumn)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun createTierRow(
        label: String,
        color: Int,
        entries: List<TierEntry>,
        gap: Int,
        posterHeight: Int,
        tileWidth: Int,
        makeText: (String, Float, Int, Boolean) -> TextView
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(gap, gap, gap, gap)
            setBackgroundColor((color and 0x00FFFFFF) or 0x33000000)
        }
        val tierLabel = makeText(label, 22f, AndroidColor.WHITE, true)
        row.addView(tierLabel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.16f))

        val horizontal = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        val items = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        entries.forEach { entry ->
            val asset = state.anime[entry.animeId]
            val tile = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(gap / 2, 0, gap / 2, 0)
            }
            val poster = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                val path = asset?.posterPath
                if (path != null) {
                    val bitmap = runCatching { PosterImages.decode(path, 480, 640) }.getOrNull()
                    if (bitmap != null) setImageBitmap(bitmap)
                }
                if (drawable == null) setBackgroundColor(AndroidColor.rgb(48, 51, 59))
            }
            tile.addView(poster, LinearLayout.LayoutParams(tileWidth, posterHeight))
            tile.addView(
                makeText(asset?.title ?: entry.animeId, 13f, AndroidColor.WHITE, false).apply {
                    gravity = Gravity.CENTER
                    maxLines = 2
                },
                LinearLayout.LayoutParams(tileWidth, dp(42))
            )
            items.addView(tile)
        }
        horizontal.addView(items)
        row.addView(horizontal, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.84f))
        return row
    }
}
