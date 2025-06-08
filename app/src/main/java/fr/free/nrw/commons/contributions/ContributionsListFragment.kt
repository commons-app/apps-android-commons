package fr.free.nrw.commons.contributions

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.recyclerview.widget.SimpleItemAnimator
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.WikipediaInstructionsDialogFragment.Companion.newInstance
import fr.free.nrw.commons.databinding.FragmentContributionsListBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.di.NetworkingModule
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.ui.CustomFabController
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.wikidata.model.WikiSite
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject
import javax.inject.Named


/**
 * Created by root on 01.06.2018.
 */
open class ContributionsListFragment : CommonsDaggerSupportFragment(), ContributionsListContract.View,
    ContributionsListAdapter.Callback, WikipediaInstructionsDialogFragment.Callback {
    @JvmField
    @Inject
    var systemThemeUtils: SystemThemeUtils? = null

    @JvmField
    @Inject
    var controller: ContributionController? = null

    @JvmField
    @Inject
    var mediaClient: MediaClient? = null

    @JvmField
    @Inject
    var mediaDataExtractor: MediaDataExtractor? = null

    @JvmField
    @Named(NetworkingModule.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
    @Inject
    var languageWikipediaSite: WikiSite? = null

    @JvmField
    @Inject
    var contributionsListPresenter: ContributionsListPresenter? = null

    @JvmField
    @Inject
    var sessionManager: SessionManager? = null

    private var binding: FragmentContributionsListBinding? = null
    private lateinit var fabController: CustomFabController

    @VisibleForTesting
    var rvContributionsList: RecyclerView? = null

    @VisibleForTesting
    var adapter: ContributionsListAdapter? = null

    @VisibleForTesting
    var callback: Callback? = null

    private val SPAN_COUNT_LANDSCAPE = 3
    private val SPAN_COUNT_PORTRAIT = 1

    private var contributionsSize = 0
    private var userName: String? = null

    @SuppressLint("NewApi")
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            userName = requireArguments().getString(ProfileActivity.KEY_USERNAME)
        }

        if (StringUtils.isEmpty(userName)) {
            userName = sessionManager!!.userName
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContributionsListBinding.inflate(
            inflater, container, false
        )
        rvContributionsList = binding!!.contributionsList

        contributionsListPresenter!!.onAttachView(this)

        if (sessionManager!!.userName == userName) {
            binding!!.tvContributionsOfUser.visibility = View.GONE
            binding!!.fabLayout.visibility = View.VISIBLE
        } else {
            binding!!.tvContributionsOfUser.visibility = View.VISIBLE
            binding!!.tvContributionsOfUser.text =
                getString(R.string.contributions_of_user, userName)
            binding!!.fabLayout.visibility = View.GONE
        }

        initAdapter()

        // pull down to refresh only enabled for self user.
        if (sessionManager!!.userName == userName) {
            binding!!.swipeRefreshLayout.setOnRefreshListener {
                contributionsListPresenter!!.refreshList(
                    binding!!.swipeRefreshLayout
                )
            }
        } else {
            binding!!.swipeRefreshLayout.isEnabled = false
        }

        return binding!!.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null && parentFragment is ContributionsFragment) {
            callback = (parentFragment as ContributionsFragment)
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null //To avoid possible memory leak
    }

    private fun initAdapter() {
        adapter = ContributionsListAdapter(this, mediaClient!!, mediaDataExtractor!!, compositeDisposable)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fabController = CustomFabController(
            this,
            requireContext(),
            binding!!.fabPlus,
            binding!!.fabCamera,
            binding!!.fabGallery,
            binding!!.fabCustomGallery,
            controller!!
        )
        fabController.initializeLaunchers()
        initRecyclerView()
        fabController.setListeners(controller!!, requireActivity())
    }

    private fun initRecyclerView() {
        val layoutManager = GridLayoutManager(
            context,
            getSpanCount(resources.configuration.orientation)
        )
        rvContributionsList!!.layoutManager = layoutManager

        //Setting flicker animation of recycler view to false.
        val animator = rvContributionsList!!.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        contributionsListPresenter!!.setup(
            userName,
            sessionManager!!.userName == userName
        )
        contributionsListPresenter!!.contributionList?.observe(
            viewLifecycleOwner
        ) { list: PagedList<Contribution>? ->
            if (list != null) {
                contributionsSize = list.size
            }
            adapter!!.submitList(list)
            if (callback != null) {
                callback!!.notifyDataSetChanged()
            }
        }
        rvContributionsList!!.adapter = adapter
        adapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                contributionsSize = adapter!!.itemCount
                if (callback != null) {
                    callback!!.notifyDataSetChanged()
                }
                if (itemCount > 0 && positionStart == 0) {
                    if (adapter!!.getContributionForPosition(positionStart) != null) {
                        rvContributionsList!!
                            .scrollToPosition(0) //Newly upload items are always added to the top
                    }
                }
            }

            /**
             * Called whenever items in the list have changed
             * Calls viewPagerNotifyDataSetChanged() that will notify the viewpager
             */
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                if (callback != null) {
                    callback!!.viewPagerNotifyDataSetChanged()
                }
            }
        })

        //Fab close on touch outside (Scrolling or taping on item triggers this action).
        rvContributionsList!!.addOnItemTouchListener(object : OnItemTouchListener {
            /**
             * Silently observe and/or take over touch events sent to the RecyclerView before
             * they are handled by either the RecyclerView itself or its child views.
             */
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    fabController.closeFabMenuIfOpen()
                }
                return false
            }

            /**
             * Process a touch event as part of a gesture that was claimed by returning true
             * from a previous call to [.onInterceptTouchEvent].
             *
             * @param rv
             * @param e  MotionEvent describing the touch event. All coordinates are in the
             * RecyclerView's coordinate system.
             */
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                //required abstract method DO NOT DELETE
            }

            /**
             * Called when a child of RecyclerView does not want RecyclerView and its ancestors
             * to intercept touch events with [ViewGroup.onInterceptTouchEvent].
             *
             * @param disallowIntercept True if the child does not want the parent to intercept
             * touch events.
             */
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                //required abstract method DO NOT DELETE
            }
        })
    }

    private fun getSpanCount(orientation: Int): Int {
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) SPAN_COUNT_LANDSCAPE else SPAN_COUNT_PORTRAIT
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // check orientation
        binding!!.fabLayout.orientation =
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        rvContributionsList
            ?.setLayoutManager(
                GridLayoutManager(context, getSpanCount(newConfig.orientation))
            )
    }

    /**
     * Launch Custom Selector.
     */

    fun scrollToTop() {
        rvContributionsList!!.smoothScrollToPosition(0)
    }

    /**
     * Shows welcome message if user has no contributions yet i.e. new user.
     */
    override fun showWelcomeTip(numberOfUploads: Boolean) {
        binding!!.noContributionsYet.visibility =
            if (numberOfUploads) View.VISIBLE else View.GONE
    }

    /**
     * Responsible to set progress bar invisible and visible
     *
     * @param shouldShow True when contributions list should be hidden.
     */
    override fun showProgress(shouldShow: Boolean) {
        binding!!.loadingContributionsProgressBar.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    override fun showNoContributionsUI(shouldShow: Boolean) {
        binding!!.noContributionsYet.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val layoutManager = rvContributionsList
            ?.getLayoutManager() as GridLayoutManager?
        outState.putParcelable(RV_STATE, layoutManager!!.onSaveInstanceState())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (null != savedInstanceState) {
            val savedRecyclerLayoutState = savedInstanceState.getParcelable<Parcelable>(RV_STATE)
            rvContributionsList!!.layoutManager!!.onRestoreInstanceState(savedRecyclerLayoutState)
        }
    }

    override fun openMediaDetail(contribution: Int, isWikipediaPageExists: Boolean) {
        if (null != callback) { //Just being safe, ideally they won't be called when detached
            callback!!.showDetail(contribution, isWikipediaPageExists)
        }
    }

    /**
     * Handle callback for wikipedia icon clicked
     *
     * @param contribution
     */
    override fun addImageToWikipedia(contribution: Contribution?) {
        showAlertDialog(
            requireActivity(),
            getString(R.string.add_picture_to_wikipedia_article_title),
            getString(R.string.add_picture_to_wikipedia_article_desc),
            {
                if (contribution != null) {
                    showAddImageToWikipediaInstructions(contribution)
                }
            }, {})
    }

    /**
     * Display confirmation dialog with instructions when the user tries to add image to wikipedia
     *
     * @param contribution
     */
    private fun showAddImageToWikipediaInstructions(contribution: Contribution) {
        val fragmentManager = fragmentManager
        val fragment = newInstance(contribution)
        fragment.callback =
            WikipediaInstructionsDialogFragment.Callback { contribution: Contribution?, copyWikicode: Boolean ->
                onConfirmClicked(
                    contribution,
                    copyWikicode
                )
            }
        fragment.show(fragmentManager!!, "WikimediaFragment")
    }


    fun getMediaAtPosition(i: Int): Media? {
        if (adapter!!.getContributionForPosition(i) != null) {
            return adapter!!.getContributionForPosition(i)!!.media
        }
        return null
    }

    val totalMediaCount: Int
        get() = contributionsSize

    /**
     * Open the editor for the language Wikipedia
     *
     * @param contribution
     */
    override fun onConfirmClicked(contribution: Contribution?, copyWikicode: Boolean) {
        if (copyWikicode) {
            val wikicode = contribution!!.media.wikiCode
            Utils.copy("wikicode", wikicode, context)
        }

        val url =
            languageWikipediaSite!!.mobileUrl() + "/wiki/" + (contribution!!.wikidataPlace
                ?.getWikipediaPageTitle())
        Utils.handleWebUrl(context, Uri.parse(url))
    }

    fun getContributionStateAt(position: Int): Int {
        return adapter!!.getContributionForPosition(position)!!.state
    }

     interface Callback {
        fun notifyDataSetChanged()

        fun showDetail(position: Int, isWikipediaButtonDisplayed: Boolean)

        // Notify the viewpager that number of items have changed.
        fun viewPagerNotifyDataSetChanged()
    }

    companion object {
        private const val RV_STATE = "rv_scroll_state"
    }
}
