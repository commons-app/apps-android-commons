package fr.free.nrw.commons.category

import android.net.Uri
import java.util.Date

data class Category(
    var contentUri: Uri? = null,
    val name: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
    val lastUsed: Date? = null,
    var timesUsed: Int = 0
) {
    fun incTimesUsed() {
        timesUsed++
    }
}