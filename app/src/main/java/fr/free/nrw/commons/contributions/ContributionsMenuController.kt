package fr.free.nrw.commons.contributions

import android.annotation.SuppressLint
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.work.WorkInfo
import androidx.work.WorkManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.mwapi.UploadCount
import fr.free.nrw.commons.notification.NotificationActivity.Companion.startYourself
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.upload.UploadProgressActivity
import fr.free.nrw.commons.upload.worker.UploadWorker
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/** Owns the contributions notification badges and upload counts. */
class ContributionsMenuController(
    private val fragment: ContributionsFragment,
) {
    private companion object {
        // Echo's formatted badge stops at 99. A raw count above that is "99+".
        const val NOTIFICATION_BADGE_CAP = 99
    }

    /** Binds the notification and upload badges and their click actions. */
    fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        // Removing contributions menu items for ProfileActivity

        if (fragment.activity is ProfileActivity) {
            return
        }

        inflater.inflate(R.menu.contribution_activity_notification_menu, menu)

        val notificationsMenuItem = menu.findItem(R.id.notifications)
        val notification = notificationsMenuItem.actionView
        fragment.notificationCount = notification!!.findViewById(R.id.notification_count_badge)
        val uploadMenuItem = menu.findItem(R.id.upload_tab)
        val uploadMenuItemActionView = uploadMenuItem.actionView
        fragment.pendingUploadsCountTextView =
            uploadMenuItemActionView!!.findViewById(
                R.id.pending_uploads_count_badge,
            )
        fragment.uploadsErrorTextView =
            uploadMenuItemActionView.findViewById(
                R.id.uploads_error_count_badge,
            )
        fragment.pendingUploadsImageView =
            uploadMenuItemActionView.findViewById(
                R.id.pending_uploads_image_view,
            )
        setupUploadClickListeners()
        notification.setOnClickListener { view: View? ->
            fragment.context?.let {
                startYourself(
                    it,
                    "unread",
                )
            }
        }
    }

    /** Opens upload progress from each upload badge. */
    private fun setupUploadClickListeners() {
        val listener =
            View.OnClickListener {
                fragment.startActivity(Intent(fragment.context, UploadProgressActivity::class.java))
            }
        fragment.pendingUploadsImageView?.setOnClickListener(listener)
        fragment.pendingUploadsCountTextView?.setOnClickListener(listener)
        fragment.uploadsErrorTextView?.setOnClickListener(listener)
    }

    /** Loads the unread count and updates the notification badge. */
    @SuppressLint("CheckResult")
    fun setNotificationCount() {
        fragment.subscriptions.add(
            fragment.notificationController!!
                .getUnreadCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { unreadCount: Int ->
                        this.initNotificationViews(unreadCount)
                    },
                    { throwable: Throwable? ->
                        Timber.e(
                            throwable,
                            "Error occurred while loading notifications",
                        )
                    },
                ),
        )
    }

    /**
     * Sets the count for the upload icon based on the number of pending and failed contributions.
     */
    fun setUploadIconCount() {
        fragment.contributionController!!.pendingContributions
        fragment.contributionController!!.pendingContributionList!!.observe(
            fragment.viewLifecycleOwner,
            Observer<PagedList<Contribution>> { list: PagedList<Contribution> ->
                updatePendingIcon(list.size)
            },
        )
        fragment.contributionController!!.failedContributions
        fragment.contributionController!!.failedContributionList!!.observe(
            fragment.viewLifecycleOwner,
            Observer<PagedList<Contribution>> { list: PagedList<Contribution> ->
                updateErrorIcon(list.size)
            },
        )
    }

    /** Shows the unread count, capped at the notification badge limit. */
    private fun initNotificationViews(unreadCount: Int) {
        Timber.d("Number of notifications is %d", unreadCount)
        if (unreadCount <= 0) {
            fragment.notificationCount!!.visibility = View.GONE
        } else {
            fragment.notificationCount!!.visibility = View.VISIBLE
            // Echo stops counting at 100 and shows "99+" above 99.
            fragment.notificationCount!!.text =
                if (unreadCount > NOTIFICATION_BADGE_CAP) {
                    fragment.getString(R.string.notification_count_cap)
                } else {
                    unreadCount.toString()
                }
        }
    }

    /**
     * Updates the visibility and text of the pending uploads count TextView based on the given
     * count.
     *
     * @param pendingCount The number of pending uploads.
     */
    fun updatePendingIcon(pendingCount: Int) {
        if (fragment.pendingUploadsCountTextView != null) {
            if (pendingCount != 0) {
                fragment.pendingUploadsCountTextView!!.visibility = View.VISIBLE
                fragment.pendingUploadsCountTextView!!.text = pendingCount.toString()
            } else {
                fragment.pendingUploadsCountTextView!!.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * Updates the visibility and text of the error uploads TextView based on the given count.
     *
     * @param errorCount The number of error uploads.
     */
    fun updateErrorIcon(errorCount: Int) {
        if (fragment.uploadsErrorTextView != null) {
            if (errorCount != 0) {
                fragment.uploadsErrorTextView!!.visibility = View.VISIBLE
                fragment.uploadsErrorTextView!!.text = errorCount.toString()
            } else {
                fragment.uploadsErrorTextView!!.visibility = View.GONE
            }
        }
    }

    /** Fetches the account upload count for the main activity. */
    fun setUploadCount() {
        fragment.userStatsClient
            ?.getUploadCount(fragment.sessionManager?.currentAccount!!.name)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.let {
                fragment.subscriptions.add(
                    it
                        .subscribe(
                            { uploadCount: UploadCount -> this.displayUploadCount(uploadCount) },
                            { t: Throwable? -> Timber.e(t, "Fetching upload count failed") },
                        ),
                )
            }
    }

    /** Displays the upload count while the activity is available. */
    private fun displayUploadCount(uploadCount: UploadCount) {
        if (fragment.requireActivity().isFinishing ||
            fragment.resources == null
        ) {
            return
        }

        (fragment.activity as MainActivity).setNumOfUploads(uploadCount)
    }

    /**
     * this function updates the number of contributions
     */
    fun upDateUploadCount() {
        fragment.context?.let {
            WorkManager
                .getInstance(it)
                .getWorkInfosForUniqueWorkLiveData(UploadWorker::class.java.simpleName)
                .observe(
                    fragment.viewLifecycleOwner,
                ) { workInfos: List<WorkInfo?> ->
                    if (workInfos.size > 0) {
                        setUploadCount()
                    }
                }
        }
    }

    // Temporarily disabled, see issue [https://github.com/commons-app/apps-android-commons/issues/5847]
    // Sets the visibility of the upload icon based on the number of failed and pending
    // contributions.
    //
    //    public void setUploadIconVisibility() {
    //        contributionController.getFailedAndPendingContributions();
    //        contributionController.failedAndPendingContributionList.observe(getViewLifecycleOwner(),
    //            list -> {
    //                updateUploadIcon(list.size());
    //            });
    //    }

    // /**
    //  * Temporarily disabled. See issue [#5847](https://github.com/commons-app/apps-android-commons/issues/5847)
    //  * @param count The number of pending uploads.
    //  */
    // public void updateUploadIcon(int count) {
    //    public void updateUploadIcon(int count) {
    //        if (pendingUploadsImageView != null) {
    //            if (count != 0) {
    //                pendingUploadsImageView.setVisibility(View.VISIBLE);
    //            } else {
    //                pendingUploadsImageView.setVisibility(View.GONE);
    //            }
    //        }
    //    }
}
