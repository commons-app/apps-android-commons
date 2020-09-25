package fr.free.nrw.commons.explore.categories.sub

import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CATEGORY_PREFIX
import fr.free.nrw.commons.explore.categories.PageableCategoryFragment
import javax.inject.Inject


class SubCategoriesFragment : PageableCategoryFragment() {

    @Inject lateinit var presenter: SubCategoriesPresenter

    override val injectedPresenter
        get() = presenter

    override fun getEmptyText(query: String) = getString(R.string.no_subcategory_found)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onQueryUpdated("$CATEGORY_PREFIX${arguments!!.getString("categoryName")!!}")
    }
}
