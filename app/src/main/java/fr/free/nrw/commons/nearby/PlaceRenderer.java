package fr.free.nrw.commons.nearby;

import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.transition.TransitionManager;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.pedrogomez.renderers.Renderer;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

class PlaceRenderer extends Renderer<Place> {
    //private static boolean isAnyItemOpen = false;

    @BindView(R.id.tvName) TextView tvName;
    @BindView(R.id.tvDesc) TextView tvDesc;
    @BindView(R.id.distance) TextView distance;
    @BindView(R.id.icon) ImageView icon;
    @BindView(R.id.buttonLayout) LinearLayout buttonLayout;
    @BindView(R.id.cameraButton) LinearLayout cameraButton;
    @BindView(R.id.galeryButton) LinearLayout galeryButton;
    @BindView(R.id.directionsButton) LinearLayout directionsButton;

    private View view;
    private static ArrayList<LinearLayout> openedItems;


    PlaceRenderer(){
        Log.d("nesli","renderer created");
        openedItems = new ArrayList<>();
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        view = layoutInflater.inflate(R.layout.item_place, viewGroup, false);
        return view;
    }

    @Override
    protected void setUpView(View view) {
        ButterKnife.bind(this, view);
    }

    @Override
    protected void hookListeners(View view) {

        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Renderer", "clicked");
                TransitionManager.beginDelayedTransition(buttonLayout);

                if(buttonLayout.isShown()){
                    closeLayout(buttonLayout);
                }else {
                    openLayout(buttonLayout);
                }

            }
        };
        view.setOnClickListener(listener);
        view.requestFocus();
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(!hasFocus && buttonLayout.isShown()){
                    closeLayout(buttonLayout);
                }else if(hasFocus && !buttonLayout.isShown()) {
                    listener.onClick(view);
                }
            }
        });
    }

    private void closeLayout(LinearLayout buttonLayout){
        buttonLayout.setVisibility(View.GONE);
    }

    private void openLayout(LinearLayout buttonLayout){
        buttonLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void render() {
        Place place = getContent();
        tvName.setText(place.name);
        String descriptionText = place.getDescription().getText();
        if (descriptionText.equals("?")) {
            descriptionText = getContext().getString(R.string.no_description_found);
        }
        tvDesc.setText(descriptionText);
        distance.setText(place.distance);
        icon.setImageResource(place.getDescription().getIcon());

        directionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng location = new LatLng(place.location.getLatitude()
                        , place.location.getLongitude(), 0);
                //Open map app at given position
                Uri gmmIntentUri = Uri.parse(
                        "geo:0,0?q=" + location.getLatitude() + "," + location.getLongitude());
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

                if (mapIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                    view.getContext().startActivity(mapIntent);
                }
            }
        });

    }

    private void startActivity(Intent intent){

    }
}