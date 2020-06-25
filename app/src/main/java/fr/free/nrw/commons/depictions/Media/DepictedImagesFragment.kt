package fr.free.nrw.commons.depictions.Media

import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.explore.media.PageableMediaFragment
import javax.inject.Inject

class DepictedImagesFragment : PageableMediaFragment(), DepictedImagesContract.View {
    @Inject
    lateinit var presenter: DepictedImagesContract.Presenter

    override val injectedPresenter
        get() = presenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onQueryUpdated(arguments!!.getString("entityId")!!)
    }

    override fun onItemClicked(position: Int) {
        (activity as WikidataItemDetailsActivity).onMediaClicked(position)
    }

    override fun notifyViewPager() {
        (activity as WikidataItemDetailsActivity).viewPagerNotifyDataSetChanged()
    }
}
