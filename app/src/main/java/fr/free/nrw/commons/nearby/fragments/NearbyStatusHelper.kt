package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import com.google.android.material.snackbar.Snackbar
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.navtab.NavTab
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.wikidata.WikidataEditListener.WikidataP18EditListener

/**
 * Dependencies and providers required by [NearbyStatusHelper].
 */
data class NearbyStatusDependencies(
    val activityProvider: () -> Activity? = { null },
    val contextProvider: () -> Context? = { null },
    val bindingProvider: () -> FragmentNearbyParentBinding? = { null },
    val presenterProvider: () -> NearbyParentFragmentPresenter? = { null },
    val locationManagerProvider: () -> LocationServiceManager? = { null },
    val readyCallbackConsumer: ((NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback?) -> Unit)? = null,
    val backPressInterceptor: (() -> Boolean)? = null,
)

/**
 * Handles network connectivity broadcast monitoring, snackbar status messages,
 * progress bar visibility, and Wikidata edit callback for NearbyParentFragment.
 */
class NearbyStatusHelper :
    NearbyParentFragmentContract.StatusControl,
    WikidataP18EditListener {
    private var activityProvider: () -> Activity? = { null }
    private var contextProvider: () -> Context? = { null }
    private var bindingProvider: () -> FragmentNearbyParentBinding? = { null }
    private var presenterProvider: () -> NearbyParentFragmentPresenter? = { null }
    private var locationManagerProvider: () -> LocationServiceManager? = { null }
    private var backPressInterceptor: (() -> Boolean)? = null
    private var readyCallbackConsumer: (
        (
            NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback?,
        ) -> Unit
    )? = null

    private var snackbar: Snackbar? = null
    var isNetworkErrorOccurred: Boolean = false
        private set
    private var hasShownOfflinePinsMessage: Boolean = false
    var broadcastReceiver: BroadcastReceiver? = null
        private set

    fun dismissSnackbar() {
        snackbar?.dismiss()
        snackbar = null
    }

    /**
     * Initializes the helper with required providers and configures the broadcast receiver.
     */
    fun init(dependencies: NearbyStatusDependencies) {
        this.activityProvider = dependencies.activityProvider
        this.contextProvider = dependencies.contextProvider
        this.bindingProvider = dependencies.bindingProvider
        this.presenterProvider = dependencies.presenterProvider
        this.locationManagerProvider = dependencies.locationManagerProvider
        this.readyCallbackConsumer = dependencies.readyCallbackConsumer
        dependencies.backPressInterceptor?.let { this.backPressInterceptor = it }
        initNetworkBroadcastReceiver()
    }

    /**
     * Checks whether internet connection is currently established.
     */
    override fun isNetworkConnectionEstablished(): Boolean = isInternetConnectionEstablished(activityProvider())

    /**
     * Updates the snackbar message when offline pins are shown.
     * Avoids flickering between offline messages while panning; switches once per offline session.
     *
     * @param offlinePinsShown Whether there are pins currently being shown on map.
     */
    override fun updateSnackbar(offlinePinsShown: Boolean) {
        if (!isNetworkErrorOccurred || snackbar == null) {
            return
        }
        if (offlinePinsShown && !hasShownOfflinePinsMessage) {
            snackbar?.setText(R.string.nearby_showing_pins_offline)
            hasShownOfflinePinsMessage = true
        }
    }

    /**
     * Sets map progress bar visibility.
     */
    override fun setProgressBarVisibility(isVisible: Boolean) {
        bindingProvider()?.mapProgressBar?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    /**
     * Handles back button press delegation for the fragment.
     */
    override fun backButtonClicked(): Boolean {
        if (backPressInterceptor?.invoke() == true) {
            return true
        }
        (activityProvider() as? MainActivity)?.setSelectedItemId(NavTab.NEARBY.code())
        return presenterProvider()?.backButtonClicked() ?: false
    }

    /**
     * Sets callback notified when the fragment instance views and overlays are ready.
     */
    override fun setNearbyParentFragmentInstanceReadyCallback(
        callback: NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback?,
    ) {
        readyCallbackConsumer?.invoke(callback)
    }

    /**
     * Opens Wiki Loves Monuments guide in a web browser.
     */
    override fun onLearnMoreClicked() {
        val activity = activityProvider() ?: return
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(NearbyParentFragment.WLM_URL)
        activity.startActivity(intent)
    }

    /**
     * Handles callback when Wikidata edit is successful by updating map and list.
     */
    override fun onWikidataEditSuccessful() {
        if (presenterProvider() != null && locationManagerProvider() != null) {
            presenterProvider()?.updateMapAndList(LocationChangeType.MAP_UPDATED)
        }
    }

    /**
     * Adds network broadcast receiver to recognize connection established
     */
    private fun initNetworkBroadcastReceiver() {
        broadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    val activity = activityProvider() ?: return
                    if (isInternetConnectionEstablished(activity)) {
                        if (isNetworkErrorOccurred) {
                            presenterProvider()?.updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
                            isNetworkErrorOccurred = false
                        }
                        snackbar?.dismiss()
                        snackbar = null
                        // Avoid flickering between offline messages while panning; switch once per offline session.
                        hasShownOfflinePinsMessage = false
                    } else {
                        val root = bindingProvider()?.root
                        if (snackbar == null && root != null) {
                            val newSnackbar = Snackbar.make(root, R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                            hasShownOfflinePinsMessage = false
                            setProgressBarVisibility(false)
                            newSnackbar.show()
                            snackbar = newSnackbar
                        }
                        isNetworkErrorOccurred = true
                    }
                }
            }
    }
}
