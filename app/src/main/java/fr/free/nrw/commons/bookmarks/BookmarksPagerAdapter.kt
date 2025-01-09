package fr.free.nrw.commons.bookmarks

import android.content.Context
import android.os.Bundle
import android.widget.ListAdapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

import java.util.ArrayList

import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment


class BookmarksPagerAdapter(
    fm: FragmentManager,
    private val context: Context,
    onlyPictures: Boolean
) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val pages: ArrayList<BookmarkPages> = ArrayList()

    init {
        val picturesBundle = Bundle().apply {
            putString("categoryName", context.getString(R.string.title_page_bookmarks_pictures))
            putInt("order", 0)
        }
        pages.add(
            BookmarkPages(
                BookmarkListRootFragment(picturesBundle, this),
                context.getString(R.string.title_page_bookmarks_pictures)
            )
        )

        if (!onlyPictures) {
            // Add the location fragment if onlyPictures is false
            val locationBundle = Bundle().apply {
                putString("categoryName", context.getString(
                    R.string.title_page_bookmarks_locations
                ))
                putInt("order", 1)
            }
            pages.add(
                BookmarkPages(
                    BookmarkListRootFragment(locationBundle, this),
                    context.getString(R.string.title_page_bookmarks_locations)
                )
            )

            locationBundle.putInt("orderItem", 2)
            pages.add(
                BookmarkPages(
                    BookmarkListRootFragment(locationBundle, this),
                    context.getString(R.string.title_page_bookmarks_items)
                )
            )
        }
        val categoriesBundle = Bundle().apply {
            putString("categoryName", context.getString(R.string.title_page_bookmarks_categories))
            putInt("order", 3)
        }

        pages.add(
            BookmarkPages(
                BookmarkListRootFragment(categoriesBundle, this),
                context.getString(R.string.title_page_bookmarks_categories)
            )
        )

        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Fragment {
        return pages[position].page!!
    }

    override fun getCount(): Int {
        return pages.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return pages[position].title
    }

    /**
     * Return the adapter used to display the picture gridview
     * @return adapter
     */
    fun getMediaAdapter(): ListAdapter? {
        val fragment = (pages[0].page as BookmarkListRootFragment).listFragment
                as BookmarkPicturesFragment
        return fragment.getAdapter()
    }

    /**
     * Update the pictures list for the bookmark fragment
     */
    fun requestPictureListUpdate() {
        val fragment = (pages[0].page as BookmarkListRootFragment).listFragment as BookmarkPicturesFragment
        fragment.onResume()
    }
}
