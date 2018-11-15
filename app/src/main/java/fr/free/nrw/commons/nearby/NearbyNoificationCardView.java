package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.SwipeDismissBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.widget.Toast;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

/**
 * Custom card view for nearby notification card view on main screen, above contributions list
 */
public class NearbyNoificationCardView  extends CardView{

    private static final float MINIMUM_THRESHOLD_FOR_SWIPE = 100;
    private Context context;

    private Button permissionRequestButton;
    private Button retryButton;
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

    private void init() {
        View rootView = inflate(context, R.layout.nearby_card_view, this);

        permissionRequestButton = rootView.findViewById(R.id.permission_request_button);
        retryButton = rootView.findViewById(R.id.retry_button);
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
        if (((MainActivity)context).prefs.getBoolean("displayNearbyCardView", true)) {
            this.setVisibility(VISIBLE);
        } else {
            this.setVisibility(GONE);
        }
    }


    private void setActionListeners() {
        this.setOnClickListener(view -> ((MainActivity)context).viewPager.setCurrentItem(1));

        this.setOnTouchListener(
                (v, event) -> {
                    boolean isSwipe = false;
                    float deltaX=0.0f;
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            x1 = event.getX();
                            break;
                        case MotionEvent.ACTION_UP:
                            x2 = event.getX();
                            deltaX = x2 - x1;
                            if (deltaX < 0) {
                                //Right to left swipe
                                isSwipe = true;
                            } else if (deltaX > 0) {
                                //Left to right swipe
                                isSwipe = true;
                            }
                            break;
                    }
                    if (isSwipe && (pixelToDp(Math.abs(deltaX)) > MINIMUM_THRESHOLD_FOR_SWIPE)) {
                        v.setVisibility(GONE);
                        // Save shared preference for nearby card view accordingly
                        ((MainActivity) context).prefs.edit()
                                .putBoolean("displayNearbyCardView", false).apply();
                        ViewUtil.showLongToast(context, getResources().getString(R.string.nearby_notification_dismiss_message));
                        return true;
                    }
                    return false;
                });
    }

    private float pixelToDp(float pixels) {
        return (pixels / Resources.getSystem().getDisplayMetrics().density);
    }

    public void displayRetryButton() {
        cardViewVisibilityState = CardViewVisibilityState.RETRY;

        contentLayout.setVisibility(GONE);
        permissionRequestButton.setVisibility(GONE);
        retryButton.setVisibility(VISIBLE);

        retryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!((MainActivity)context).isFinishing()) {
                    ((ContributionsFragment)(((MainActivity) context).contributionsActivityPagerAdapter.getItem(0))).locationManager.registerLocationManager();
                }
            }
        });
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
            retryButton.setVisibility(GONE);
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
            permissionRequestButton.setVisibility(GONE);
            retryButton.setVisibility(GONE);
            contentLayout.setVisibility(VISIBLE);
            // Set visibility of elements in content layout once it become visible
            progressBar.setVisibility(VISIBLE);
            notificationTitle.setVisibility(GONE);
            notificationDistance.setVisibility(GONE);
            notificationIcon.setVisibility(GONE);

            permissionRequestButton.setVisibility(GONE);

            Handler nearbyNotificationHandler = new Handler();
            Runnable nearbyNotificationRunnable = new Runnable() {
                @Override
                public void run() {
                    if (cardViewVisibilityState != NearbyNoificationCardView.CardViewVisibilityState.READY
                            || cardViewVisibilityState != NearbyNoificationCardView.CardViewVisibilityState.ASK_PERMISSION
                            || cardViewVisibilityState != NearbyNoificationCardView.CardViewVisibilityState.INVISIBLE) {
                        // If after 30 seconds, card view is not ready
                        displayRetryButton();
                    }
                }
            };
            nearbyNotificationHandler.postDelayed(nearbyNotificationRunnable, 30000);
        }
    }

    /**
     * Pass place information to views.
     * @param isClosestNearbyPlaceFound false if there are no close place
     * @param place Closes place where we will get information from
     */
    public void updateContent(boolean isClosestNearbyPlaceFound, Place place) {
        if (this.getVisibility() == GONE) {
            return; // If nearby card view is invisible because of preferences, do nothing
        }
        cardViewVisibilityState = CardViewVisibilityState.READY;
        permissionRequestButton.setVisibility(GONE);
        retryButton.setVisibility(GONE);
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
                    retryButton.setVisibility(GONE);
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
                    retryButton.setVisibility(GONE);
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
        RETRY,
        INVISIBLE,
        ASK_PERMISSION,
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
