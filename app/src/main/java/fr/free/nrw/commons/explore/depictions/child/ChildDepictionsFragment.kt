package fr.free.nrw.commons.explore.depictions.child

import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.depictions.PageableDepictionsFragment
import javax.inject.Inject


class ChildDepictionsFragment: PageableDepictionsFragment() {
    @Inject
    lateinit var presenter: ChildDepictionsPresenter

    override val injectedPresenter
    get() = presenter

    override fun getEmptyText(query: String) =
        getString(R.string.no_child_classes, arguments!!.getString("wikidataItemName")!!)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onQueryUpdated(arguments!!.getString("entityId")!!)
    }
}
