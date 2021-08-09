package fr.free.nrw.commons.explore.depictions.media

import android.os.Bundle
import android.util.Log
import android.view.View
import fr.free.nrw.commons.explore.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.explore.media.PageableMediaFragment
import javax.inject.Inject

class DepictedImagesFragment : PageableMediaFragment() {
    @Inject
    lateinit var presenter: DepictedImagesPresenter

    override val injectedPresenter
        get() = presenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("abcde", "ID "+arguments!!.getString("entityId")!!)
        onQueryUpdated(arguments!!.getString("entityId")!!)
    }
}
