package fr.free.nrw.commons.explore.depictions.parent

import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.depictions.PageableDepictionsFragment
import javax.inject.Inject


class ParentDepictionsFragment : PageableDepictionsFragment() {
    @Inject
    lateinit var presenter: ParentDepictionsPresenter

    override val injectedPresenter
        get() = presenter

    override fun getEmptyText(query: String) =
        getString(R.string.no_parent_classes, arguments!!.getString("wikidataItemName")!!)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onQueryUpdated(arguments!!.getString("entityId")!!)
    }
}
