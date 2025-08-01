package fr.free.nrw.commons.bookmarks

import android.content.Context
import android.widget.ListAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment

class BookmarksPagerAdapter internal constructor(
    fm: FragmentManager, context: Context, onlyPictures: Boolean
) : FragmentPagerAdapter(fm) {
    private val pages = mutableListOf<BookmarkPages>()

    /**
     * Default Constructor
     * @param fm
     * @param context
     * @param onlyPictures is true if the fragment requires only BookmarkPictureFragment
     * (i.e. when no user is logged in).
     */
    init {
        pages.add(
            BookmarkPages(
                BookmarkListRootFragment(
                    bundleOf(
                        "categoryName" to context.getString(R.string.title_page_bookmarks_pictures),
                        "order" to 0
                    ), this
                ), context.getString(R.string.title_page_bookmarks_pictures)
            )
        )
        if (!onlyPictures) {
            // if onlyPictures is false we also add the location fragment.
            val locationBundle = bundleOf(
                "categoryName" to context.getString(R.string.title_page_bookmarks_locations),
                "order" to 1
            )

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
        pages.add(
            BookmarkPages(
                BookmarkListRootFragment(
                    bundleOf(
                        "categoryName" to context.getString(R.string.title_page_bookmarks_categories),
                        "order" to 3
                    ), this),
                context.getString(R.string.title_page_bookmarks_categories)
            )
        )
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Fragment = pages[position].page!!

    override fun getCount(): Int = pages.size

    override fun getPageTitle(position: Int): CharSequence? = pages[position].title

    /**
     * Return the Adapter used to display the picture gridview
     * @return adapter
     */
    val mediaAdapter: ListAdapter?
        get() = (((pages[0].page as BookmarkListRootFragment).listFragment) as BookmarkPicturesFragment).getAdapter()
}
