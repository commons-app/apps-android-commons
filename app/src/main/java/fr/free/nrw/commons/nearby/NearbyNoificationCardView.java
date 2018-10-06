package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.SwipeDismissBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import timber.log.Timber;

/**
 * Custom card view for nearby notification card view on main screen, above contributions list
 */
public class NearbyNoificationCardView  extends CardView{

    private Context context;

    private Button permissionRequestButton;
    private RelativeLayout contentLayout;
    private TextView notificationTitle;
    private TextView notificationDistance;
    private ImageView notificationIcon;
    private ProgressBar progressBar;

    public CardViewVisibilityState cardViewVisibilityState;

    public PermissionType permissionType;

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
        // Add swipe and dismiss property
        SwipeDismissBehavior swipeDismissBehavior = new SwipeDismissBehavior();
        swipeDismissBehavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY);
        swipeDismissBehavior.setListener(new SwipeDismissBehavior.OnDismissListener() {
            @Override
            public void onDismiss(View view) {
                /**
                 * Only dismissing view results a space after dismissed view. Since, we need to
                 * make view invisible at all.
                 */
                NearbyNoificationCardView.this.setVisibility(GONE);
                // Save shared preference for nearby card view accordingly
                ((ContributionsActivity) context).prefs.edit().putBoolean("displayNearbyCardView", false).apply();
            }

            @Override
            public void onDragStateChanged(int state) {

            }
        });
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) this.getLayoutParams();
        layoutParams.setBehavior(swipeDismissBehavior);

        // If you don't setVisibility after getting layout params, then you will se an empty space in place of nerabyNotificationCardView
        if (((ContributionsActivity)context).prefs.getBoolean("displayNearbyCardView", true)) {
            this.setVisibility(VISIBLE);
        } else {
            this.setVisibility(GONE);
        }
    }


    private void setActionListeners() {
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ContributionsActivity)context).viewPager.setCurrentItem(1);
            }
        });
    }

    public void displayPermissionRequestButton(boolean isPermissionRequestButtonNeeded) {
        if (isPermissionRequestButtonNeeded) {
            Log.d("deneme","called1");
            cardViewVisibilityState = CardViewVisibilityState.ASK_PERMISSION;
            contentLayout.setVisibility(GONE);
            permissionRequestButton.setVisibility(VISIBLE);

            if (permissionType == PermissionType.ENABLE_LOCATION_PERMISSON) {

                permissionRequestButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!((ContributionsActivity)context).isFinishing()) {
                            ((ContributionsActivity) context).locationManager.requestPermissions((ContributionsActivity) context);
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
                                            ((ContributionsActivity) context).startActivityForResult(callGPSSettingIntent, 1);
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
            Log.d("deneme","called2");
            permissionRequestButton.setVisibility(GONE);
            contentLayout.setVisibility(VISIBLE);
            // Set visibility of elements in content layout once it become visible
            progressBar.setVisibility(VISIBLE);
            notificationTitle.setVisibility(GONE);
            notificationDistance.setVisibility(GONE);
            notificationIcon.setVisibility(GONE);

            permissionRequestButton.setVisibility(GONE);
        }
    }

    public void updateContent(boolean isClosestNearbyPlaceFound, Place place) {
        Log.d("deneme","called3");
        if (this.getVisibility() == GONE) {
            Log.d("deneme7","nearby card view visiblity was gone, we are in update content");
            return; // If nearby card view is invisible because of preferences, do nothing
        }
        cardViewVisibilityState = CardViewVisibilityState.READY;
        permissionRequestButton.setVisibility(GONE);
        contentLayout.setVisibility(VISIBLE);
        // Make progress bar invisible once data is ready
        progressBar.setVisibility(GONE);
        // And content views visible since they are ready
        notificationTitle.setVisibility(VISIBLE);
        notificationDistance.setVisibility(VISIBLE);
        notificationIcon.setVisibility(VISIBLE);

        Log.d("deneme","called4"+this.getVisibility()+" place is:"+place.name);


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

    public enum CardViewVisibilityState {
        LOADING,
        READY,
        INVISIBLE,
        ASK_PERMISSION,
    }


    public enum PermissionType {
        ENABLE_GPS,
        ENABLE_LOCATION_PERMISSON, // For only after Marsmallow
        NO_PERMISSION_NEEDED
    }
}
