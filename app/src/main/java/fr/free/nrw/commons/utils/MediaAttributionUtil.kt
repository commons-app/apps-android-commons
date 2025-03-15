package fr.free.nrw.commons.utils

import android.content.Context
import android.icu.text.ListFormatter
import android.os.Build
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.media.IdAndLabels
import java.util.Locale

object MediaAttributionUtil {
    fun getTagLine(media: Media, context: Context): String {
        val uploader = media.user
        val author = media.getAttributedAuthor()
        return if (author.isNullOrEmpty()) {
            context.getString(R.string.image_uploaded_by, uploader)
        } else if (author == uploader) {
            context.getString(R.string.image_tag_line_created_and_uploaded_by, author)
        } else {
            context.getString(
                R.string.image_tag_line_created_by_and_uploaded_by,
                author,
                uploader
            )
        }
    }

    fun getCreatorName(idAndLabels: List<IdAndLabels>): String? {
        val locale = Locale.getDefault()
        val names = idAndLabels.map{ x -> x.getLocalizedLabel(locale.language)}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val formatter = ListFormatter.getInstance(locale)
            return formatter.format(names)
        } else {
            return names.joinToString(", ")
        }
    }

}