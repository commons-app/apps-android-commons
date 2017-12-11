package fr.free.nrw.commons.nearby;

import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

class PlaceRenderer extends Renderer<Place> {

    @BindView(R.id.tvName) TextView tvName;
    @BindView(R.id.tvDesc) TextView tvDesc;
    @BindView(R.id.distance) TextView distance;
    @BindView(R.id.icon) ImageView icon;
    @BindView(R.id.buttonLayout)
    LinearLayout buttonLayout;
    private Animation animationUp;
    private Animation animationDown;
    private final int COUNTDOWN_RUNNING_TIME = 300;
    private static ArrayList<LinearLayout> openedItems;


    PlaceRenderer(){
        openedItems = new ArrayList<>();
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.item_place, viewGroup, false);
    }

    @Override
    protected void setUpView(View view) {
        ButterKnife.bind(this, view);
        animationUp = AnimationUtils.loadAnimation(getContext(),R.anim.slide_up);
        animationDown = AnimationUtils.loadAnimation(getContext(),R.anim.slide_down);
    }

    @Override
    protected void hookListeners(View view) {
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

    private void closeLayout(LinearLayout buttonLayout) {

        buttonLayout.startAnimation(animationUp);
        CountDownTimer countDownTimerStatic = new CountDownTimer(COUNTDOWN_RUNNING_TIME
                , 16) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                buttonLayout.setVisibility(View.GONE);
            }
        };
        countDownTimerStatic.start();
    }

    private void openLayout(LinearLayout buttonLayout){
        buttonLayout.setVisibility(View.VISIBLE);
        buttonLayout.startAnimation(animationDown);
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
    }
}
