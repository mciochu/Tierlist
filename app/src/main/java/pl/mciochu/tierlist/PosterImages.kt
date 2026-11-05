package pl.mciochu.tierlist

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.max

/** Decode poster thumbnails near their display size instead of loading full camera images. */
object PosterImages {
    fun decode(path: String, maxWidth: Int = 420, maxHeight: Int = 560): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth &&
            bounds.outHeight / (sample * 2) >= maxHeight
        ) {
            sample *= 2
        }

        return BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply { inSampleSize = max(1, sample) }
        )
    }
}
