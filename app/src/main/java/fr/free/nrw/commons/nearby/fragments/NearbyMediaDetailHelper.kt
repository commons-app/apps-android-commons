package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

private const val TOTAL_MEDIA_COUNT = 2
private const val MEDIA_DETAIL_FRAGMENT_TAG = "MediaDetailFragmentTag"
private const val NEARBY_PARENT_FRAGMENT_TAG = "Nearby_Parent_Fragment_Tag"
private const val FILE_PREFIX = "File:"

/**
 * Handles media detail display, media fetching, and MediaDetailPagerFragment transactions
 * for NearbyParentFragment.
 */
class NearbyMediaDetailHelper : MediaDetailProvider {
    private var mediaClientProvider: () -> MediaClient? = { null }
    private var childFragmentManagerProvider: () -> FragmentManager? = { null }
    private var bindingProvider: () -> FragmentNearbyParentBinding? = { null }
    private var activityProvider: () -> Activity? = { null }
    private var compositeDisposableProvider: () -> CompositeDisposable? = { null }

    var media: Media? = null
    var mediaDetails: MediaDetailPagerFragment? = null

    /**
     * Initializes the helper with required providers.
     */
    fun init(
        mediaClientProvider: () -> MediaClient?,
        childFragmentManagerProvider: () -> FragmentManager?,
        bindingProvider: () -> FragmentNearbyParentBinding?,
        activityProvider: () -> Activity?,
        compositeDisposableProvider: () -> CompositeDisposable?,
    ) {
        this.mediaClientProvider = mediaClientProvider
        this.childFragmentManagerProvider = childFragmentManagerProvider
        this.bindingProvider = bindingProvider
        this.activityProvider = activityProvider
        this.compositeDisposableProvider = compositeDisposableProvider
    }

    /**
     * Handles media image clicks by fetching media details from the API.
     */
    fun handleMediaClick(imageName: String) {
        val decodedImageName = URLDecoder.decode(imageName, StandardCharsets.UTF_8.toString())
        val mediaClient = mediaClientProvider() ?: return
        val disposable = compositeDisposableProvider()

        val subscription =
            mediaClient
                .getMedia("$FILE_PREFIX$decodedImageName")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mediaResponse ->
                    if (mediaResponse != null) {
                        // Create a Media object from the response
                        media = createMediaFromResponse(mediaResponse)
                        // Remove existing fragment before showing new details
                        val existingDetails = mediaDetails
                        if (existingDetails != null && existingDetails.isAdded) {
                            removeFragment(existingDetails)
                        }
                        showMediaDetails()
                    } else {
                        Timber.e("Fetched media is null for image: $decodedImageName")
                    }
                }, { throwable ->
                    Timber.e(throwable, "Error fetching media for image: $decodedImageName")
                })
        disposable?.add(subscription)
    }

    /**
     * Instantiates and attaches a MediaDetailPagerFragment into the coordinator layout.
     */
    fun showMediaDetails() {
        val binding = bindingProvider()
        binding?.map?.setMultiTouchControls(false)
        binding?.map?.isClickable = false

        val newMediaDetails = MediaDetailPagerFragment.newInstance(false, true)
        mediaDetails = newMediaDetails

        val fragmentManager = childFragmentManagerProvider() ?: return
        val transaction = fragmentManager.beginTransaction()

        val fragmentContainer = fragmentManager.findFragmentById(R.id.coordinator_layout)
        if (fragmentContainer != null) {
            transaction.hide(fragmentContainer)
        }

        // Replace instead of add to ensure new fragment is used
        transaction.replace(R.id.coordinator_layout, newMediaDetails, MEDIA_DETAIL_FRAGMENT_TAG)
        transaction.addToBackStack(NEARBY_PARENT_FRAGMENT_TAG).commit()
        fragmentManager.executePendingTransactions()

        (activityProvider() as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (newMediaDetails.isAdded) {
            newMediaDetails.showImage(0)
        } else {
            Timber.e("Error: MediaDetailPagerFragment is NOT added")
        }
    }

    /**
     * Returns the Media object at the specified position.
     */
    override fun getMediaAtPosition(i: Int): Media? = media

    /**
     * Returns the total media count available for the provider.
     */
    override fun getTotalMediaCount(): Int = TOTAL_MEDIA_COUNT

    /**
     * Returns the contribution state at the given position.
     */
    override fun getContributionStateAt(position: Int): Int? = null

    /**
     * Refreshes the currently displayed media details.
     */
    override fun refreshNominatedMedia(index: Int) {
        val existingDetails = mediaDetails
        val mapClickable = bindingProvider()?.map?.isClickable
        if (existingDetails != null && mapClickable == false) {
            removeFragment(existingDetails)
            showMediaDetails()
        }
    }

    /**
     * Removes the given fragment from childFragmentManager.
     */
    fun removeFragment(fragment: Fragment) {
        val fragmentManager = childFragmentManagerProvider() ?: return
        fragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()
        fragmentManager.executePendingTransactions()
    }

    /**
     * Handles back press for media detail view; dismisses details and restores map state.
     */
    fun handleBackPress(): Boolean {
        val details = mediaDetails
        if (details != null && details.isVisible) {
            removeFragment(details)

            val binding = bindingProvider()
            binding?.coordinatorLayout?.visibility = View.VISIBLE
            binding?.map?.setMultiTouchControls(true)
            binding?.map?.isClickable = true

            val fragmentManager = childFragmentManagerProvider()
            if (fragmentManager != null) {
                val transaction = fragmentManager.beginTransaction()
                val fragmentContainer = fragmentManager.findFragmentById(R.id.coordinator_layout)
                if (fragmentContainer != null) {
                    transaction.show(fragmentContainer)
                }
                transaction.commit()
                fragmentManager.executePendingTransactions()
            }

            val activity = activityProvider() as? MainActivity
            activity?.showTabs()
            activity?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            return true
        }
        return false
    }

    private fun createMediaFromResponse(mediaResponse: Media): Media =
        Media(
            pageId = mediaResponse.pageId ?: UUID.randomUUID().toString(),
            thumbUrl = mediaResponse.thumbUrl,
            imageUrl = mediaResponse.imageUrl,
            filename = mediaResponse.filename,
            fallbackDescription = mediaResponse.fallbackDescription,
            dateUploaded = mediaResponse.dateUploaded,
            license = mediaResponse.license,
            licenseUrl = mediaResponse.licenseUrl,
            author = mediaResponse.author,
            user = mediaResponse.user,
            categories = mediaResponse.categories,
            coordinates = mediaResponse.coordinates,
            captions = mediaResponse.captions ?: emptyMap(),
            descriptions = mediaResponse.descriptions ?: emptyMap(),
            depictionIds = mediaResponse.depictionIds ?: emptyList(),
            categoriesHiddenStatus = mediaResponse.categoriesHiddenStatus ?: emptyMap(),
            mimeType = mediaResponse.mimeType,
            mediaType = mediaResponse.mediaType,
        )
}
