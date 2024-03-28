package fr.free.nrw.commons.quiz;

import android.app.Activity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to group to or more radio buttons to ensure
 * that at a particular time only one of them is selected
 */
public class RadioGroupHelper {

    public List<CompoundButton> radioButtons = new ArrayList<>();

    /**
     * Constructor to group radio buttons
     * @param radios
     */
    public RadioGroupHelper(RadioButton... radios) {
        super();
        for (RadioButton rb : radios) {
            add(rb);
        }
    }

    /**
     * Constructor to group radio buttons
     * @param activity
     * @param radiosIDs
     */
    public RadioGroupHelper(Activity activity, int... radiosIDs) {
        this(activity.findViewById(android.R.id.content),radiosIDs);
    }

    /**
     * Constructor to group radio buttons
     * @param rootView
     * @param radiosIDs
     */
    public RadioGroupHelper(View rootView, int... radiosIDs) {
        super();
        for (int radioButtonID : radiosIDs) {
            add(rootView.findViewById(radioButtonID));
        }
    }

    private void add(CompoundButton button){
        this.radioButtons.add(button);
        button.setOnClickListener(onClickListener);
    }

    /**
     * listener to ensure only one of the radio button is selected
     */
    View.OnClickListener onClickListener = v -> {
        for (CompoundButton rb : radioButtons) {
            if (rb != v) {
                rb.setChecked(false);
            }
        }
    };
}
