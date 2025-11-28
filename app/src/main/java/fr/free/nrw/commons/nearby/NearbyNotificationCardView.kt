package fr.free.nrw.commons.nearby

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.nearby.NearbyNotificationCardView.CardViewVisibilityState.READY
import fr.free.nrw.commons.utils.SwipableCardView
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import timber.log.Timber

/**
 * Custom card view for nearby notification card view on main screen, above contributions list
 */
class NearbyNotificationCardView : SwipableCardView {
    var permissionRequestButton: Button? = null
    private var contentLayout: LinearLayout? = null
    private var notificationTitle: TextView? = null
    private var notificationDistance: TextView? = null
    private var notificationIcon: ImageView? = null
    private var notificationCompass: ImageView? = null
    private var progressBar: ProgressBar? = null
    var cardViewVisibilityState: CardViewVisibilityState
    var permissionType: PermissionType? = null

    constructor(context: Context) : super(context) {
        cardViewVisibilityState = CardViewVisibilityState.INVISIBLE
        initUI()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        cardViewVisibilityState = CardViewVisibilityState.INVISIBLE
        initUI()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        cardViewVisibilityState = CardViewVisibilityState.INVISIBLE
        initUI()
    }

    /**
     * Initializes views and action listeners
     */
    private fun initUI() {
        val rootView = inflate(context, R.layout.nearby_card_view, this)

        permissionRequestButton = rootView.findViewById(R.id.permission_request_button)
        contentLayout = rootView.findViewById(R.id.content_layout)

        notificationTitle = rootView.findViewById(R.id.nearby_title)
        notificationDistance = rootView.findViewById(R.id.nearby_distance)

        notificationIcon = rootView.findViewById(R.id.nearby_icon)
        notificationCompass = rootView.findViewById(R.id.nearby_compass)

        progressBar = rootView.findViewById(R.id.progressBar)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // If you don't setVisibility after getting layout params, then you will se an empty space in place of nearby NotificationCardView
        if (context is MainActivity && (context as MainActivity).defaultKvStore.getBoolean("displayNearbyCardView", true) && cardViewVisibilityState == READY) {
            visibility = VISIBLE
        } else {
            visibility = GONE
        }
    }


    private fun setActionListeners(place: Place?) {
        setOnClickListener { (context as MainActivity).centerMapToPlace(place) }
    }

    override fun onSwipe(view: View): Boolean {
        view.visibility = GONE
        // Save shared preference for nearby card view accordingly
        (context as MainActivity).defaultKvStore.putBoolean("displayNearbyCardView", false)
        showLongToast(
            context,
            resources.getString(R.string.nearby_notification_dismiss_message)
        )
        return true
    }

    /**
     * Time is up, data for card view is not ready, so do not display it
     */
    private fun errorOccurred() {
        visibility = GONE
    }

    /**
     * Data for card view is ready, display card view
     */
    private fun succeeded() {
        visibility = VISIBLE
    }

    /**
     * Pass place information to views
     */
    fun updateContent(place: Place) {
        Timber.d("Update nearby card notification content")
        visibility = VISIBLE
        cardViewVisibilityState = READY
        permissionRequestButton!!.visibility = GONE
        contentLayout!!.visibility = VISIBLE
        // Make progress bar invisible once data is ready
        progressBar!!.visibility = GONE
        setActionListeners(place)
        // And content views visible since they are ready
        notificationTitle!!.visibility = VISIBLE
        notificationDistance!!.visibility = VISIBLE
        notificationIcon!!.setVisibility(VISIBLE)
        notificationTitle!!.text = place.name
        notificationDistance!!.text = place.distance
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            /*
              Sometimes we need to preserve previous state of notification card view without getting
              any data from user. Ie. wen user came back from Media Details fragment to Contrib List
              fragment, we need to know what was the state of card view, and set it to exact same state.
             */
            when (cardViewVisibilityState) {
                READY -> {
                    permissionRequestButton!!.visibility = GONE
                    contentLayout!!.visibility = VISIBLE
                    // Make progress bar invisible once data is ready
                    progressBar!!.visibility = GONE
                    // And content views visible since they are ready
                    notificationTitle!!.visibility = VISIBLE
                    notificationDistance!!.visibility = VISIBLE
                    notificationIcon!!.setVisibility(VISIBLE)
                    notificationCompass!!.setVisibility(VISIBLE)
                }

                CardViewVisibilityState.LOADING -> {
                    permissionRequestButton!!.visibility = GONE
                    contentLayout!!.visibility = VISIBLE
                    // Set visibility of elements in content layout once it become visible
                    progressBar!!.visibility = VISIBLE
                    notificationTitle!!.visibility = GONE
                    notificationDistance!!.visibility = GONE
                    notificationIcon!!.setVisibility(GONE)
                    notificationCompass!!.setVisibility(GONE)
                    permissionRequestButton!!.visibility = GONE
                }

                CardViewVisibilityState.ASK_PERMISSION -> {
                    contentLayout!!.visibility = GONE
                    permissionRequestButton!!.visibility = VISIBLE
                }

                else -> {}
            }
        }
    }

    /**
     * This states will help us to preserve progress bar and content layout states
     */
    enum class CardViewVisibilityState {
        LOADING,
        READY,
        INVISIBLE,
        ASK_PERMISSION,
        ERROR_OCCURRED
    }

    /**
     * We need to know which kind of permission we need to request, then update permission request
     * button action accordingly
     */
    enum class PermissionType {
        ENABLE_GPS,
        ENABLE_LOCATION_PERMISSION,  // For only after Marshmallow
        NO_PERMISSION_NEEDED
    }

    /**
     * Rotates the compass arrow in tandem with the rotation of device
     */
    fun rotateCompass(rotateDegree: Float, direction: Float) {
        notificationCompass!!.rotation = -(rotateDegree - direction)
    }
}
