package fr.free.nrw.commons.category

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CategoryItem(val name: String, val description: String?,
                        val thumbnail: String?, var isSelected: Boolean) : Parcelable {

    override fun toString(): String {
        return "CategoryItem: '$name'"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CategoryItem

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
