package fr.free.nrw.commons.quiz;

import android.app.Activity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.List;

public class RadioGroupHelper {

    public List<CompoundButton> radioButtons = new ArrayList<>();

    public RadioGroupHelper(RadioButton... radios) {
        super();
        for (RadioButton rb : radios) {
            add(rb);
        }
    }

    public RadioGroupHelper(Activity activity, int... radiosIDs) {
        this(activity.findViewById(android.R.id.content),radiosIDs);
    }

    public RadioGroupHelper(View rootView, int... radiosIDs) {
        super();
        for (int radioButtonID : radiosIDs) {
            add((RadioButton)rootView.findViewById(radioButtonID));
        }
    }

    private void add(CompoundButton button){
        this.radioButtons.add(button);
        button.setOnClickListener(onClickListener);
    }

    View.OnClickListener onClickListener = v -> {
        for (CompoundButton rb : radioButtons) {
            if(rb != v) rb.setChecked(false);
        }
    };
}