package fr.free.nrw.commons.category

import android.app.Activity
import android.os.Bundle
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment
import fr.free.nrw.commons.explore.categories.parent.ParentCategoriesFragment
import fr.free.nrw.commons.explore.categories.sub.SubCategoriesFragment

/** Configures the media, subcategory and parent tabs for a category. */
internal class CategoryDetailsTabs(
    private val activity: Activity,
    private val viewPagerAdapter: ViewPagerAdapter,
    private val viewModel: CategoryDetailsViewModel,
) {
    /**
     * This activity contains 3 tabs and a viewpager. This method is used to set the titles of tab,
     * Set the fragments according to the tab selected in the viewPager.
     */
    fun setTabs(categoryName: String?): CategoriesMediaFragment {
        val categoriesMediaFragment = CategoriesMediaFragment()
        val subCategoryListFragment = SubCategoriesFragment()
        val parentCategoriesFragment = ParentCategoriesFragment()
        if (activity.intent != null && categoryName != null) {
            val arguments =
                Bundle().apply {
                    putString("categoryName", categoryName)
                }
            categoriesMediaFragment.arguments = arguments
            subCategoryListFragment.arguments = arguments
            parentCategoriesFragment.arguments = arguments

            viewModel.onCheckIfBookmarked(categoryName)
        }

        viewPagerAdapter.setTabs(
            R.string.title_for_media to categoriesMediaFragment,
            R.string.title_for_subcategories to subCategoryListFragment,
            R.string.title_for_parent_categories to parentCategoriesFragment,
        )
        viewPagerAdapter.notifyDataSetChanged()
        return categoriesMediaFragment
    }

    /**
     * Gets the passed categoryName from the intents and displays it as the page title
     */
    fun setPageTitle() {
        activity.intent?.getStringExtra("categoryName")?.let {
            activity.title = it
        }
    }
}
