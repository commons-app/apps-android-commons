package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.utils.SwipableCardView;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

/**
 * Custom card view for nearby notification card view on main screen, above contributions list
 */
public class NearbyNoificationCardView  extends SwipableCardView {

    private Context context;

    private Button permissionRequestButton;
    private RelativeLayout contentLayout;
    private TextView notificationTitle;
    private TextView notificationDistance;
    private ImageView notificationIcon;
    private ProgressBar progressBar;

    public CardViewVisibilityState cardViewVisibilityState;

    public PermissionType permissionType;

    float x1,x2;

    public NearbyNoificationCardView(@NonNull Context context) {
        super(context);
        this.context = context;
        cardViewVisibilityState = CardViewVisibilityState.INVISIBLE;
        init();
    }

    public NearbyNoificationCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        cardViewVisibilityState = CardViewVisibilityState.INVISIBLE;
        init();
    }

    public NearbyNoificationCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        cardViewVisibilityState = CardViewVisibilityState.INVISIBLE;
        init();
    }

    /**
     * Initializes views and action listeners
     */
    private void init() {
        View rootView = inflate(context, R.layout.nearby_card_view, this);

        permissionRequestButton = rootView.findViewById(R.id.permission_request_button);
        contentLayout = rootView.findViewById(R.id.content_layout);

        notificationTitle = rootView.findViewById(R.id.nearby_title);
        notificationDistance = rootView.findViewById(R.id.nearby_distance);

        notificationIcon = rootView.findViewById(R.id.nearby_icon);

        progressBar = rootView.findViewById(R.id.progressBar);

        setActionListeners();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // If you don't setVisibility after getting layout params, then you will se an empty space in place of nerabyNotificationCardView
        if (((MainActivity)context).prefs.getBoolean("displayNearbyCardView", true) && this.cardViewVisibilityState == NearbyNoificationCardView.CardViewVisibilityState.READY) {
            this.setVisibility(VISIBLE);
        } else {
            this.setVisibility(GONE);
        }
    }


    private void setActionListeners() {
        this.setOnClickListener(view -> ((MainActivity)context).viewPager.setCurrentItem(1));
    }

    @Override public boolean onSwipe(View view) {
        view.setVisibility(GONE);
        // Save shared preference for nearby card view accordingly
        ((MainActivity) context).prefs.edit().putBoolean("displayNearbyCardView", false).apply();
        ViewUtil.showLongToast(context,
            getResources().getString(R.string.nearby_notification_dismiss_message));
        return true;
    }

    /**
     * Sets permission request button visible and content layout invisible, then adds correct
     * permission request actions to permission request button according to PermissionType enum
     * @param isPermissionRequestButtonNeeded true if permissions missing
     */
    public void displayPermissionRequestButton(boolean isPermissionRequestButtonNeeded) {
        if (isPermissionRequestButtonNeeded) {
            cardViewVisibilityState = CardViewVisibilityState.ASK_PERMISSION;
            contentLayout.setVisibility(GONE);
            permissionRequestButton.setVisibility(VISIBLE);

            if (permissionType == PermissionType.ENABLE_LOCATION_PERMISSON) {

                permissionRequestButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!((MainActivity)context).isFinishing()) {
                            ((MainActivity) context).locationManager.requestPermissions((MainActivity) context);
                        }
                    }
                });

            } else if (permissionType == PermissionType.ENABLE_GPS) {

                permissionRequestButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new AlertDialog.Builder(context)
                                .setMessage(R.string.gps_disabled)
                                .setCancelable(false)
                                .setPositiveButton(R.string.enable_gps,
                                        (dialog, id) -> {
                                            Intent callGPSSettingIntent = new Intent(
                                                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            Timber.d("Loaded settings page");
                                            ((MainActivity) context).startActivityForResult(callGPSSettingIntent, 1);
                                        })
                                .setNegativeButton(R.string.menu_cancel_upload, (dialog, id) -> {
                                    dialog.cancel();
                                    displayPermissionRequestButton(true);
                                })
                                .create()
                                .show();
                    }
                });
            }


        } else {
            cardViewVisibilityState = CardViewVisibilityState.LOADING;

            this.setVisibility(GONE);
            Handler nearbyNotificationHandler = new Handler();
            Runnable nearbyNotificationRunnable = new Runnable() {
                @Override
                public void run() {
                    if (cardViewVisibilityState != NearbyNoificationCardView.CardViewVisibilityState.READY
                            && cardViewVisibilityState != NearbyNoificationCardView.CardViewVisibilityState.ASK_PERMISSION
                            && cardViewVisibilityState != NearbyNoificationCardView.CardViewVisibilityState.INVISIBLE) {
                        // If after 30 seconds, card view is not ready
                        errorOcured();
                    } else {
                        suceeded();
                    }
                }
            };
            nearbyNotificationHandler.postDelayed(nearbyNotificationRunnable, 30000);
        }
    }

    /**
     * Time is up, data for card view is not ready, so do not display it
     */
    private void errorOcured() {
        this.setVisibility(GONE);
    }

    /**
     * Data for card view is ready, display card view
     */
    private void suceeded() {
        this.setVisibility(VISIBLE);
    }

    /**
     * Pass place information to views.
     * @param isClosestNearbyPlaceFound false if there are no close place
     * @param place Closes place where we will get information from
     */
    public void updateContent(boolean isClosestNearbyPlaceFound, Place place) {
        Timber.d("Update nearby card notification content");
        this.setVisibility(VISIBLE);
        cardViewVisibilityState = CardViewVisibilityState.READY;
        permissionRequestButton.setVisibility(GONE);
        contentLayout.setVisibility(VISIBLE);
        // Make progress bar invisible once data is ready
        progressBar.setVisibility(GONE);
        // And content views visible since they are ready
        notificationTitle.setVisibility(VISIBLE);
        notificationDistance.setVisibility(VISIBLE);
        notificationIcon.setVisibility(VISIBLE);

        if (isClosestNearbyPlaceFound) {
            notificationTitle.setText(place.name);
            notificationDistance.setText(place.distance);
        } else {
            notificationDistance.setText("");
            notificationTitle.setText(R.string.no_close_nearby);
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            /**
             * Sometimes we need to preserve previous state of notification card view without getting
             * any data from user. Ie. wen user came back from Media Details fragment to Contrib List
             * fragment, we need to know what was the state of card view, and set it to exact same state.
             */
            switch (cardViewVisibilityState) {
                case READY:
                    permissionRequestButton.setVisibility(GONE);
                    contentLayout.setVisibility(VISIBLE);
                    // Make progress bar invisible once data is ready
                    progressBar.setVisibility(GONE);
                    // And content views visible since they are ready
                    notificationTitle.setVisibility(VISIBLE);
                    notificationDistance.setVisibility(VISIBLE);
                    notificationIcon.setVisibility(VISIBLE);
                    break;
                case LOADING:
                    permissionRequestButton.setVisibility(GONE);
                    contentLayout.setVisibility(VISIBLE);
                    // Set visibility of elements in content layout once it become visible
                    progressBar.setVisibility(VISIBLE);
                    notificationTitle.setVisibility(GONE);
                    notificationDistance.setVisibility(GONE);
                    notificationIcon.setVisibility(GONE);
                    permissionRequestButton.setVisibility(GONE);
                    break;
                case ASK_PERMISSION:
                    contentLayout.setVisibility(GONE);
                    permissionRequestButton.setVisibility(VISIBLE);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * This states will help us to preserve progress bar and content layout states
     */
    public enum CardViewVisibilityState {
        LOADING,
        READY,
        INVISIBLE,
        ASK_PERMISSION,
        ERROR_OCURED
    }

    /**
     * We need to know which kind of permission we need to request, then update permission request
     * button action accordingly
     */
    public enum PermissionType {
        ENABLE_GPS,
        ENABLE_LOCATION_PERMISSON, // For only after Marsmallow
        NO_PERMISSION_NEEDED
    }
}
