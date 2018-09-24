package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.SwipeDismissBehavior;
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

    public NearbyNoificationCardView(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    public NearbyNoificationCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public NearbyNoificationCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
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
        permissionRequestButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!((ContributionsActivity)context).isFinishing()) {
                    ((ContributionsActivity) context).locationManager.requestPermissions((ContributionsActivity) context);
                }
            }
        });
    }

    public void displayPermissionRequestButton(boolean isPermissionRequestButtonNeeded) {
        if (isPermissionRequestButtonNeeded) {
            Log.d("deneme","called1");
            contentLayout.setVisibility(GONE);
            permissionRequestButton.setVisibility(VISIBLE);
        } else {
            Log.d("deneme","called2");
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
            notificationTitle.setText("No nearby places found around you");
        }
    }
}
