package fr.free.nrw.commons.media

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryDetailsActivity
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Objects

/**
 * Gets new categories from the WikiText and updates it on the UI
 *
 * @param s WikiText
 */
internal fun MediaDetailFragment.updateCategoryList(s: String) {
    val allCategories: MutableList<String> = ArrayList()
    var i: Int = s.indexOf("[[Category:")
    while (i != -1) {
        val category: String = s.substring(i + "[[Category:".length, s.indexOf("]]", i))
        allCategories.add(category)
        i = s.indexOf("]]", i)
        i = s.indexOf("[[Category:", i)
    }
    media!!.categories = allCategories
    if (allCategories.isEmpty()) {
        // Stick in a filler element.
        allCategories.add(getString(R.string.detail_panel_cats_none))
    }
    if (sessionManager.isUserLoggedIn) {
        binding.categoryEditButton.visibility = View.VISIBLE
    }
    rebuildCatList(allCategories)
}

/**
 * Updates the categories
 */
internal fun MediaDetailFragment.refreshCategoryList() {
    val allCategories: MutableList<String> =
        ArrayList(
            media?.addedCategories!!,
        )
    media!!.categories = allCategories
    if (allCategories.isEmpty()) {
        // Stick in a filler element.
        allCategories.add(getString(R.string.detail_panel_cats_none))
    }

    rebuildCatList(allCategories)
}

/** Loads wikitext for the category editor. */
internal fun MediaDetailFragment.onCategoryEditButtonClicked() {
    binding.progressBarEditCategory.visibility = View.VISIBLE
    binding.categoryEditButton.visibility = View.GONE
    wikiText
}

private val MediaDetailFragment.wikiText: Unit
    /**
     * Gets WikiText from the server and send it to catgory editor
     */
    get() {
        viewDisposables.add(
            mediaDataExtractor
                .getCurrentWikiText(
                    Objects.requireNonNull(media?.filename!!),
                ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { s: String? -> gotoCategoryEditor(s!!) },
                    { t: Throwable? -> Timber.e(t) },
                ),
        )
    }

/**
 * Opens the category editor
 *
 * @param s WikiText
 */
internal fun MediaDetailFragment.gotoCategoryEditor(s: String) {
    binding.categoryEditButton.visibility = View.VISIBLE
    binding.progressBarEditCategory.visibility = View.GONE
    val categoriesFragment: Fragment = UploadCategoriesFragment()
    val bundle = Bundle()
    bundle.putParcelable("Existing_Categories", media)
    bundle.putString("WikiText", s)
    categoriesFragment.arguments = bundle
    val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
    transaction.replace(R.id.mediaDetailFrameLayout, categoriesFragment)
    transaction.addToBackStack(null)
    transaction.commit()
}

/** Rebuilds the rendered list of categories. */
internal fun MediaDetailFragment.rebuildCatList(categories: List<String>) {
    binding.mediaDetailCategoryContainer.removeAllViews()
    for (category: String in categories) {
        binding.mediaDetailCategoryContainer.addView(
            buildCatLabel(
                sanitise(category),
                binding.mediaDetailCategoryContainer,
            ),
        )
    }
}

// As per issue #1826(see https://github.com/commons-app/apps-android-commons/issues/1826),
// some categories come suffixed with strings prefixed with |. As per the discussion
// that was meant for alphabetical sorting of the categories and can be safely removed.

/** Removes the category sort key. */
internal fun MediaDetailFragment.sanitise(category: String): String {
    val indexOfPipe: Int = category.indexOf('|')
    if (indexOfPipe != -1) {
        // Removed everything after '|'
        return category.substring(0, indexOfPipe)
    }
    return category
}

/** Builds a category label that opens the category. */
internal fun MediaDetailFragment.buildCatLabel(
    catName: String,
    categoryContainer: ViewGroup,
): View {
    val item: View =
        LayoutInflater
            .from(context)
            .inflate(R.layout.detail_category_item, categoryContainer, false)
    val textView: TextView = item.findViewById(R.id.mediaDetailCategoryItemText)

    textView.text = catName
    if (getString(R.string.detail_panel_cats_none) != catName) {
        textView.setOnClickListener {
            // Open Category Details page
            val intent = Intent(context, CategoryDetailsActivity::class.java)
            intent.putExtra("categoryName", catName)
            requireContext().startActivity(intent)
        }
    }
    return item
}
