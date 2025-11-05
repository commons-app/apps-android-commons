package fr.free.nrw.commons.media

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.snackbar.Snackbar
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.bookmarks.models.Bookmark
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentMediaDetailPagerBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.profile.ProfileActivity.Companion.startYourself
import fr.free.nrw.commons.utils.ClipboardUtils.copy
import fr.free.nrw.commons.utils.DownloadUtils.downloadMedia
import fr.free.nrw.commons.utils.ImageUtils.setAvatarFromImageUrl
import fr.free.nrw.commons.utils.ImageUtils.setWallpaperFromImageUrl
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.utils.ViewUtil.showShortSnackbar
import fr.free.nrw.commons.utils.handleWebUrl
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.URL
import java.util.concurrent.Callable
import javax.inject.Inject
import androidx.core.net.toUri

class MediaDetailPagerFragment : CommonsDaggerSupportFragment(), OnPageChangeListener,
    MediaDetailFragment.Callback {
    @JvmField
    @Inject
    var bookmarkDao: BookmarkPicturesDao? = null

    @JvmField
    @Inject
    var okHttpJsonApiClient: OkHttpJsonApiClient? = null

    @JvmField
    @Inject
    var sessionManager: SessionManager? = null

    var binding: FragmentMediaDetailPagerBinding? = null
    var editable: Boolean = false
    var isFeaturedImage: Boolean = false
    var isWikipediaButtonDisplayed: Boolean = false
    var adapter: MediaDetailAdapter? = null
    var bookmark: Bookmark? = null
    var mediaDetailProvider: MediaDetailProvider? = null
    var isFromFeaturedRootFragment: Boolean = false
    var position: Int = 0

    /**
     * ProgressBar used to indicate the loading status of media items.
     */
    var imageProgressBar: ProgressBar? = null

    var removedItems: ArrayList<Int> = ArrayList()

    fun clearRemoved() = removedItems.clear()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMediaDetailPagerBinding.inflate(inflater, container, false)
        binding!!.mediaDetailsPager.addOnPageChangeListener(this)
        // Initialize the ProgressBar by finding it in the layout
        imageProgressBar = binding!!.root.findViewById(R.id.itemProgressBar)
        adapter = MediaDetailAdapter(this, childFragmentManager)

        // ActionBar is now supported in both activities - if this crashes something is quite wrong
        val actionBar = (activity as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
        } else {
            throw AssertionError("Action bar should not be null!")
        }

        // If fragment is associated with ProfileActivity, then hide the tabLayout
        if (activity is ProfileActivity) {
            (activity as ProfileActivity).setTabLayoutVisibility(false)
        }

        binding!!.mediaDetailsPager.adapter = adapter

        if (savedInstanceState != null) {
            val pageNumber = savedInstanceState.getInt("current-page")
            binding!!.mediaDetailsPager.setCurrentItem(pageNumber, false)
            requireActivity().invalidateOptionsMenu()
        }
        adapter!!.notifyDataSetChanged()

        return binding!!.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current-page", binding!!.mediaDetailsPager.currentItem)
        outState.putBoolean("editable", editable)
        outState.putBoolean("isFeaturedImage", isFeaturedImage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable", false)
            isFeaturedImage = savedInstanceState.getBoolean("isFeaturedImage", false)
        }
        setHasOptionsMenu(true)
        initProvider()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity is MainActivity) {
            (activity as MainActivity).showTabs()
        }
        binding = null
    }

    /**
     * initialise the provider, based on from where the fragment was started, as in from an activity
     * or a fragment
     */
    private fun initProvider() {
        if (parentFragment is MediaDetailProvider) {
            mediaDetailProvider = parentFragment as MediaDetailProvider
        } else if (activity is MediaDetailProvider) {
            mediaDetailProvider = activity as MediaDetailProvider?
        } else {
            throw ClassCastException("Parent must implement MediaDetailProvider")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (activity == null) {
            Timber.d("Returning as activity is destroyed!")
            return true
        }

        val m = mediaDetailProvider!!.getMediaAtPosition(binding!!.mediaDetailsPager.currentItem)
        val mediaDetailFragment = adapter!!.currentMediaDetailFragment
        when (item.itemId) {
            R.id.menu_bookmark_current_image -> {
                val bookmarkExists = bookmarkDao!!.updateBookmark(bookmark!!)
                val snackbar = if (bookmarkExists) Snackbar.make(
                    requireView(),
                    R.string.add_bookmark,
                    Snackbar.LENGTH_LONG
                ) else Snackbar.make(
                    requireView(), R.string.remove_bookmark, Snackbar.LENGTH_LONG
                )
                snackbar.show()
                updateBookmarkState(item)
                return true
            }

            R.id.menu_copy_link -> {
                val uri = m!!.pageTitle.canonicalUri
                copy("shareLink", uri, requireContext())
                Timber.d("Copied share link to clipboard: %s", uri)
                Toast.makeText(
                    requireContext(), getString(R.string.menu_link_copied),
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }

            R.id.menu_share_current_image -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.setType("text/plain")
                shareIntent.putExtra(
                    Intent.EXTRA_TEXT, """${m!!.displayTitle} 
${m.pageTitle.canonicalUri}"""
                )
                startActivity(Intent.createChooser(shareIntent, "Share image via..."))

                //Add media detail to backstack when the share button is clicked
                //So that when the share is cancelled or completed the media detail page is on top
                // of back stack fixing:https://github.com/commons-app/apps-android-commons/issues/2296
                val supportFragmentManager = requireActivity().supportFragmentManager
                if (supportFragmentManager.backStackEntryCount < 2) {
                    supportFragmentManager
                        .beginTransaction()
                        .addToBackStack(MediaDetailPagerFragment::class.java.name)
                        .commit()
                    supportFragmentManager.executePendingTransactions()
                }
                return true
            }

            R.id.menu_browser_current_image -> {
                // View in browser
                handleWebUrl(requireContext(), m!!.pageTitle.mobileUri.toUri())
                return true
            }

            R.id.menu_download_current_image -> {
                // Download
                if (!isInternetConnectionEstablished(activity)) {
                    showShortSnackbar(requireView(), R.string.no_internet)
                    return false
                }
                downloadMedia(activity, m!!)
                return true
            }

            R.id.menu_set_as_wallpaper -> {
                // Set wallpaper
                setWallpaper(m!!)
                return true
            }

            R.id.menu_set_as_avatar -> {
                // Set avatar
                setAvatar(m!!)
                return true
            }

            R.id.menu_view_user_page -> {
                if (m?.user != null) {
                    startYourself(
                        requireActivity(), m.user!!,
                        sessionManager!!.userName != m.user
                    )
                }
                return true
            }

            R.id.menu_view_report -> {
                showReportDialog(m)
                mediaDetailFragment?.onImageBackgroundChanged(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
                return true
            }

            R.id.menu_view_set_white_background -> {
                mediaDetailFragment?.onImageBackgroundChanged(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
                return true
            }

            R.id.menu_view_set_black_background -> {
                mediaDetailFragment?.onImageBackgroundChanged(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.black
                    )
                )
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun showReportDialog(media: Media?) {
        if (media == null) {
            return
        }
        val builder = AlertDialog.Builder(requireActivity())
        val values = requireContext().resources
            .getStringArray(R.array.report_violation_options)
        builder.setTitle(R.string.report_violation)
        builder.setItems(
            R.array.report_violation_options
        ) { dialog: DialogInterface?, which: Int ->
            sendReportEmail(media, values[which])
        }
        builder.setNegativeButton(
            R.string.cancel
        ) { dialog: DialogInterface?, which: Int -> }
        builder.setCancelable(false)
        builder.show()
    }

    private fun sendReportEmail(media: Media, type: String) {
        val technicalInfo = getTechInfo(media, type)

        val feedbackIntent = Intent(Intent.ACTION_SENDTO)
        feedbackIntent.setType("message/rfc822")
        feedbackIntent.setData(Uri.parse("mailto:"))
        feedbackIntent.putExtra(
            Intent.EXTRA_EMAIL,
            arrayOf(CommonsApplication.REPORT_EMAIL)
        )
        feedbackIntent.putExtra(
            Intent.EXTRA_SUBJECT,
            CommonsApplication.REPORT_EMAIL_SUBJECT
        )
        feedbackIntent.putExtra(Intent.EXTRA_TEXT, technicalInfo)
        try {
            startActivity(feedbackIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.no_email_client, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTechInfo(media: Media, type: String): String {
        val builder = StringBuilder()

        builder.append("Report type: ")
            .append(type)
            .append("\n\n")

        builder.append("Image that you want to report: ")
            .append(media.imageUrl)
            .append("\n\n")

        builder.append("User that you want to report: ")
            .append(media.user)
            .append("\n\n")

        if (sessionManager!!.userName != null) {
            builder.append("Your username: ")
                .append(sessionManager!!.userName)
                .append("\n\n")
        }

        builder.append("Violation reason: ")
            .append("\n")

        builder.append("----------------------------------------------")
            .append("\n")
            .append("(please write reason here)")
            .append("\n")
            .append("----------------------------------------------")
            .append("\n\n")
            .append("Thank you for your report! Our team will investigate as soon as possible.")
            .append("\n")
            .append("Please note that images also have a `Nominate for deletion` button.")

        return builder.toString()
    }

    /**
     * Set the media as the device's wallpaper if the imageUrl is not null
     * Fails silently if setting the wallpaper fails
     * @param media
     */
    private fun setWallpaper(media: Media) {
        if (media.imageUrl == null || media.imageUrl!!.isEmpty()) {
            Timber.d("Media URL not present")
            return
        }
        setWallpaperFromImageUrl(requireActivity(), media.imageUrl!!.toUri())
    }

    /**
     * Set the media as user's leaderboard avatar
     * @param media
     */
    private fun setAvatar(media: Media) {
        if (media.imageUrl == null || media.imageUrl!!.isEmpty()) {
            Timber.d("Media URL not present")
            return
        }
        setAvatarFromImageUrl(
            requireActivity(), media.imageUrl!!,
            sessionManager!!.currentAccount!!.name,
            okHttpJsonApiClient!!, Companion.compositeDisposable
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!editable) { // Disable menu options for editable views
            menu.clear() // see http://stackoverflow.com/a/8495697/17865
            inflater.inflate(R.menu.fragment_image_detail, menu)
            if (binding!!.mediaDetailsPager != null) {
                val provider = mediaDetailProvider ?: return
                val position = if (isFromFeaturedRootFragment) {
                    position
                } else {
                    binding!!.mediaDetailsPager.currentItem
                }

                val m = provider.getMediaAtPosition(position)
                if (m != null) {
                    // Enable default set of actions, then re-enable different set of actions only if it is a failed contrib
                    menu.findItem(R.id.menu_browser_current_image).setEnabled(true).setVisible(true)
                    menu.findItem(R.id.menu_copy_link).setEnabled(true).setVisible(true)
                    menu.findItem(R.id.menu_share_current_image).setEnabled(true).setVisible(true)
                    menu.findItem(R.id.menu_download_current_image).setEnabled(true)
                        .setVisible(true)
                    menu.findItem(R.id.menu_bookmark_current_image).setEnabled(true)
                        .setVisible(true)
                    menu.findItem(R.id.menu_set_as_wallpaper).setEnabled(true).setVisible(true)
                    if (m.user != null) {
                        menu.findItem(R.id.menu_view_user_page).setEnabled(true).setVisible(true)
                    }

                    try {
                        val mediaUrl = URL(m.imageUrl)
                        handleBackgroundColorMenuItems({
                            BitmapFactory.decodeStream(
                                mediaUrl.openConnection().getInputStream()
                            )
                        }, menu)
                    } catch (e: Exception) {
                        Timber.e("Cant detect media transparency")
                    }

                    // Initialize bookmark object
                    bookmark = Bookmark(
                        m.filename,
                        m.getAuthorOrUser(),
                        BookmarkPicturesContentProvider.uriForName(m.filename!!)
                    )
                    updateBookmarkState(menu.findItem(R.id.menu_bookmark_current_image))
                    val contributionState = provider.getContributionStateAt(position)
                    if (contributionState != null) {
                        when (contributionState) {
                            Contribution.STATE_FAILED, Contribution.STATE_IN_PROGRESS, Contribution.STATE_QUEUED -> {
                                menu.findItem(R.id.menu_browser_current_image).setEnabled(false)
                                    .setVisible(false)
                                menu.findItem(R.id.menu_copy_link).setEnabled(false)
                                    .setVisible(false)
                                menu.findItem(R.id.menu_share_current_image).setEnabled(false)
                                    .setVisible(false)
                                menu.findItem(R.id.menu_download_current_image).setEnabled(false)
                                    .setVisible(false)
                                menu.findItem(R.id.menu_bookmark_current_image).setEnabled(false)
                                    .setVisible(false)
                                menu.findItem(R.id.menu_set_as_wallpaper).setEnabled(false)
                                    .setVisible(false)
                            }

                            Contribution.STATE_COMPLETED -> {}
                        }
                    }
                } else {
                    menu.findItem(R.id.menu_browser_current_image).setEnabled(false)
                        .setVisible(false)
                    menu.findItem(R.id.menu_copy_link).setEnabled(false)
                        .setVisible(false)
                    menu.findItem(R.id.menu_share_current_image).setEnabled(false)
                        .setVisible(false)
                    menu.findItem(R.id.menu_download_current_image).setEnabled(false)
                        .setVisible(false)
                    menu.findItem(R.id.menu_bookmark_current_image).setEnabled(false)
                        .setVisible(false)
                    menu.findItem(R.id.menu_set_as_wallpaper).setEnabled(false)
                        .setVisible(false)
                }

                if (!sessionManager!!.isUserLoggedIn) {
                    menu.findItem(R.id.menu_set_as_avatar).setVisible(false)
                }
            }
        }
    }

    /**
     * Decide wether or not we should display the background color menu items
     * We display them if the image is transparent
     * @param getBitmap
     * @param menu
     */
    private fun handleBackgroundColorMenuItems(getBitmap: Callable<Bitmap>, menu: Menu) {
        Observable.fromCallable(
            getBitmap
        ).subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer { image: Bitmap ->
                if (image.hasAlpha()) {
                    menu.findItem(R.id.menu_view_set_white_background).setVisible(true)
                        .setEnabled(true)
                    menu.findItem(R.id.menu_view_set_black_background).setVisible(true)
                        .setEnabled(true)
                }
            })
    }

    private fun updateBookmarkState(item: MenuItem) {
        val isBookmarked = bookmarkDao!!.findBookmark(bookmark)
        if (isBookmarked) {
            if (removedItems.contains(binding!!.mediaDetailsPager.currentItem)) {
                removedItems.remove(binding!!.mediaDetailsPager.currentItem)
            }
        } else {
            if (!removedItems.contains(binding!!.mediaDetailsPager.currentItem)) {
                removedItems.add(binding!!.mediaDetailsPager.currentItem)
            }
        }

        item.setIcon(if (isBookmarked) {
            R.drawable.menu_ic_round_star_filled_24px
        } else {
            R.drawable.menu_ic_round_star_border_24px
        })
    }

    fun showImage(i: Int, isWikipediaButtonDisplayed: Boolean) {
        this.isWikipediaButtonDisplayed = isWikipediaButtonDisplayed
        setViewPagerCurrentItem(i)
    }

    fun showImage(i: Int) {
        setViewPagerCurrentItem(i)
    }

    /**
     * This function waits for the item to load then sets the item to current item
     * @param position current item that to be shown
     */
    private fun setViewPagerCurrentItem(position: Int) {
        val handler = Handler(Looper.getMainLooper())
        val runnable: Runnable = object : Runnable {
            override fun run() {
                // Show the ProgressBar while waiting for the item to load
                imageProgressBar!!.visibility = View.VISIBLE
                // Check if the adapter has enough items loaded
                if (adapter!!.count > position) {
                    // Set the current item in the ViewPager
                    binding!!.mediaDetailsPager.setCurrentItem(position, false)
                    // Hide the ProgressBar once the item is loaded
                    imageProgressBar!!.visibility = View.GONE
                } else {
                    // If the item is not ready yet, post the Runnable again
                    handler.post(this)
                }
            }
        }
        // Start the Runnable
        handler.post(runnable)
    }

    /**
     * The method notify the viewpager that number of items have changed.
     */
    fun notifyDataSetChanged() {
        if (null != adapter) {
            adapter!!.notifyDataSetChanged()
        }
    }

    override fun onPageScrolled(i: Int, v: Float, i2: Int) {
        if (activity == null) {
            Timber.d("Returning as activity is destroyed!")
            return
        }

        requireActivity().invalidateOptionsMenu()
    }

    override fun onPageSelected(i: Int) {
    }

    override fun onPageScrollStateChanged(i: Int) {
    }

    fun onDataSetChanged() {
        if (null != adapter) {
            adapter!!.notifyDataSetChanged()
        }
    }

    /**
     * Called after the media is nominated for deletion
     *
     * @param index item position that has been nominated
     */
    override fun nominatingForDeletion(index: Int) {
        mediaDetailProvider!!.refreshNominatedMedia(index)
    }

    companion object {
        private val compositeDisposable = CompositeDisposable()

        /**
         * Use this factory method to create a new instance of this fragment using the provided
         * parameters.
         *
         * This method will create a new instance of MediaDetailPagerFragment and the arguments will be
         * saved to a bundle which will be later available in the [.onCreate]
         * @param editable
         * @param isFeaturedImage
         * @return
         */
        @JvmStatic
        fun newInstance(editable: Boolean, isFeaturedImage: Boolean): MediaDetailPagerFragment {
            val mediaDetailPagerFragment = MediaDetailPagerFragment()
            val args = Bundle()
            args.putBoolean("is_editable", editable)
            args.putBoolean("is_featured_image", isFeaturedImage)
            mediaDetailPagerFragment.arguments = args
            return mediaDetailPagerFragment
        }
    }
}
