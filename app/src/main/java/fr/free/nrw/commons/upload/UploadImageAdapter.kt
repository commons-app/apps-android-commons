package fr.free.nrw.commons.upload

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * The adapter used to show image upload intermediate fragments
 */
internal class UploadImageAdapter(
    fragmentManager: FragmentManager,
) : FragmentStatePagerAdapter(fragmentManager) {
    var fragments: List<UploadBaseFragment> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /** Supplies a page to the upload pager. */
    override fun getItem(position: Int): Fragment = fragments[position]

    /** Supplies a page to the upload pager. */
    override fun getCount(): Int = fragments.size

    /** Supplies a page to the upload pager. */
    override fun getItemPosition(item: Any): Int = POSITION_NONE
}
