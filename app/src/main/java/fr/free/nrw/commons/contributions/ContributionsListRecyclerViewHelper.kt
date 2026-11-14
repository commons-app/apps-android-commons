package fr.free.nrw.commons.contributions

import android.content.res.Configuration
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.recyclerview.widget.SimpleItemAnimator
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.disposables.CompositeDisposable

object ContributionsListRecyclerViewHelper {
    private const val SPAN_COUNT_LANDSCAPE = 3
    private const val SPAN_COUNT_PORTRAIT = 1

    fun createAdapter(
        callback: ContributionsListAdapter.Callback,
        mediaClient: MediaClient,
        mediaDataExtractor: MediaDataExtractor,
        compositeDisposable: CompositeDisposable,
    ): ContributionsListAdapter =
        ContributionsListAdapter(
            callback,
            mediaClient,
            mediaDataExtractor,
            compositeDisposable,
        )

    fun initializeRecyclerView(
        fragment: ContributionsListFragment,
        userName: String?,
    ) {
        val layoutManager =
            GridLayoutManager(
                fragment.context,
                spanCountForOrientation(fragment.resources.configuration.orientation),
            )
        fragment.rvContributionsList!!.layoutManager = layoutManager

        // Setting flicker animation of recycler view to false.
        val animator = fragment.rvContributionsList!!.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        fragment.contributionsListPresenter!!.setup(
            userName,
            fragment.sessionManager!!.userName == userName,
        )
        fragment.contributionsListPresenter!!.contributionList?.observe(
            fragment.viewLifecycleOwner,
        ) { list: PagedList<Contribution>? ->
            if (list != null) {
                fragment.contributionsSize = list.size
            }
            fragment.adapter!!.submitList(list)
            if (fragment.callback != null) {
                fragment.callback!!.notifyDataSetChanged()
            }
        }
        fragment.rvContributionsList!!.adapter = fragment.adapter
        fragment.adapter!!.registerAdapterDataObserver(
            object : AdapterDataObserver() {
                override fun onItemRangeInserted(
                    positionStart: Int,
                    itemCount: Int,
                ) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    fragment.contributionsSize = fragment.adapter!!.itemCount
                    if (fragment.callback != null) {
                        fragment.callback!!.notifyDataSetChanged()
                    }
                    if (itemCount > 0 && positionStart == 0) {
                        if (fragment.adapter!!.getContributionForPosition(positionStart) != null) {
                            fragment.rvContributionsList!!
                                .scrollToPosition(0) // Newly upload items are always added to the top
                        }
                    }
                }

                /**
                 * Called whenever items in the list have changed
                 * Calls viewPagerNotifyDataSetChanged() that will notify the viewpager
                 */
                override fun onItemRangeChanged(
                    positionStart: Int,
                    itemCount: Int,
                ) {
                    super.onItemRangeChanged(positionStart, itemCount)
                    if (fragment.callback != null) {
                        fragment.callback!!.viewPagerNotifyDataSetChanged()
                    }
                }
            },
        )

        // Fab close on touch outside (Scrolling or taping on item triggers this action).
        fragment.rvContributionsList!!.addOnItemTouchListener(
            object : OnItemTouchListener {
                /**
                 * Silently observe and/or take over touch events sent to the RecyclerView before
                 * they are handled by either the RecyclerView itself or its child views.
                 */
                override fun onInterceptTouchEvent(
                    rv: RecyclerView,
                    e: MotionEvent,
                ): Boolean {
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        if (fragment.fabHelper.isFabOpen) {
                            fragment.fabHelper.animateFAB(true)
                        }
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
                override fun onTouchEvent(
                    rv: RecyclerView,
                    e: MotionEvent,
                ) {
                    // required abstract method DO NOT DELETE
                }

                /**
                 * Called when a child of RecyclerView does not want RecyclerView and its ancestors
                 * to intercept touch events with [ViewGroup.onInterceptTouchEvent].
                 *
                 * @param disallowIntercept True if the child does not want the parent to intercept
                 * touch events.
                 */
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                    // required abstract method DO NOT DELETE
                }
            },
        )
    }

    fun spanCountForOrientation(orientation: Int): Int =
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            SPAN_COUNT_LANDSCAPE
        } else {
            SPAN_COUNT_PORTRAIT
        }
}
