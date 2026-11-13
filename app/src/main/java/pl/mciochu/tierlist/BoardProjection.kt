package pl.mciochu.tierlist

import android.app.Presentation
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import pl.mciochu.tierlist.data.SavedEditor
import pl.mciochu.tierlist.domain.TierEntry

/**
 * A read-only board for an external display. The ranking fills the available
 * screen; each tier gets an equal share of the height and posters scroll sideways.
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

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private fun render() {
        fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
            TextView(context).apply {
                text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER_VERTICAL
                includeFontPadding = false
            }

        val tiers = state.board.tiers.sortedBy { it.position }
        val unranked = state.board.entries.filter { it.tierId == null }.sortedBy { it.position }
        val rowCount = tiers.size + if (unranked.isNotEmpty()) 1 else 0
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        val screenHeightDp = (metrics.heightPixels / metrics.density).toInt()
        val rowHeightDp = ((screenHeightDp - 34).coerceAtLeast(1) / rowCount).coerceAtLeast(1)
        val posterHeightDp = (rowHeightDp - 28).coerceIn(28, 112)
        val tileWidthDp = (posterHeightDp * 0.68f).toInt().coerceAtLeast(24)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(2))
            setBackgroundColor(AndroidColor.rgb(16, 18, 22))
        }

        root.addView(
            text(state.board.name, 16f, AndroidColor.WHITE, true).apply {
                setPadding(dp(8), 0, dp(8), 0)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30))
        )

        tiers.forEach { tier ->
            val entries = state.board.entries.filter { it.tierId == tier.id }
                .sortedBy { it.position }
            root.addView(
                createTierRow(
                    tier.label,
                    tier.colorArgb.toInt(),
                    entries,
                    posterHeightDp,
                    tileWidthDp,
                    ::text
                ),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }

        if (unranked.isNotEmpty()) {
            root.addView(
                createTierRow(
                    "…",
                    AndroidColor.rgb(75, 81, 93),
                    unranked,
                    posterHeightDp,
                    tileWidthDp,
                    ::text
                ),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }

        setContentView(root)
    }

    private fun createTierRow(
        label: String,
        color: Int,
        entries: List<TierEntry>,
        posterHeightDp: Int,
        tileWidthDp: Int,
        makeText: (String, Float, Int, Boolean) -> TextView
    ): LinearLayout {
        val gap = dp(3)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(gap, gap, gap, gap)
            setBackgroundColor(AndroidColor.rgb(27, 32, 40))
        }

        val tierLabel = makeText(label, 21f, AndroidColor.rgb(16, 18, 22), true).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(color)
        }
        row.addView(
            tierLabel,
            LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT)
        )

        val horizontal = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
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
                setPadding(dp(2), 0, dp(2), 0)
            }
            val poster = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                val path = asset?.posterPath
                if (path != null) {
                    val bitmap = runCatching {
                        PosterImages.decode(path, 480, 640)
                    }.getOrNull()
                    if (bitmap != null) setImageBitmap(bitmap)
                }
                if (drawable == null) {
                    setBackgroundColor(AndroidColor.rgb(48, 54, 64))
                }
            }
            tile.addView(
                poster,
                LinearLayout.LayoutParams(dp(tileWidthDp), dp(posterHeightDp))
            )

            if (posterHeightDp >= 48) {
                tile.addView(
                    makeText(
                        asset?.title ?: entry.animeId,
                        10f,
                        AndroidColor.WHITE,
                        false
                    ).apply {
                        gravity = Gravity.CENTER
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    },
                    LinearLayout.LayoutParams(dp(tileWidthDp + 4), dp(18))
                )
            }

            items.addView(tile)
        }

        horizontal.addView(items)
        row.addView(
            horizontal,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )
        return row
    }
}
