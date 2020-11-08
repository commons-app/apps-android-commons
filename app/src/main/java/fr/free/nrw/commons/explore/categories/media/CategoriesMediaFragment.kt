package fr.free.nrw.commons.explore.categories.media

import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.category.CATEGORY_PREFIX
import fr.free.nrw.commons.explore.media.PageableMediaFragment
import javax.inject.Inject


class CategoriesMediaFragment : PageableMediaFragment() {

    @Inject
    lateinit var presenter: CategoryMediaPresenter

    override val injectedPresenter
        get() = presenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onQueryUpdated("$CATEGORY_PREFIX${arguments!!.getString("categoryName")!!}")
    }
}
