package fr.free.nrw.commons.explore.categories.media

import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.category.CATEGORY_PREFIX
import fr.free.nrw.commons.explore.media.PageableMediaFragment
import javax.inject.Inject


// New file, e.g. category/CategoryDetailsMediaFragment.kt
class CategoryDetailsMediaFragment : CategoriesMediaFragment() {
    override fun hideInitialLoadProgress() {
        super.hideInitialLoadProgress()
        showContributionFab();
    }

    override fun showInitialLoadInProgress() {
        super.showInitialLoadInProgress()
        hideContributionFab();
    }
}
