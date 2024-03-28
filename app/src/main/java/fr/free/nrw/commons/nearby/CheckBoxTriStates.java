package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;

import java.util.List;

import fr.free.nrw.commons.R;

/**
 * Base on https://stackoverflow.com/a/40939367/3950497 answer.
 */
public class CheckBoxTriStates extends AppCompatCheckBox {

    static public final int UNKNOWN = -1;

    static public final int UNCHECKED = 0;

    static public final int CHECKED = 1;

    private int state=UNKNOWN;

    private Callback callback;

    public interface Callback{
        void filterByMarkerType(@Nullable List<Label> selectedLabels, int state, boolean b, boolean b1);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * This is the listener set to the super class which is going to be evoke each
     * time the check state has changed.
     */
    private final OnCheckedChangeListener privateListener = new CompoundButton.OnCheckedChangeListener() {

        // checkbox status is changed from uncheck to checked.
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (state) {
                case UNKNOWN:
                    setState(UNCHECKED);;
                    break;
                case UNCHECKED:
                    setState(CHECKED);
                    break;
                case CHECKED:
                    setState(UNKNOWN);
                    break;
            }
        }
    };

    /**
     * Holds a reference to the listener set by a client, if any.
     */
    private OnCheckedChangeListener clientListener;


    public CheckBoxTriStates(Context context) {
        super(context);
        init();
    }

    public CheckBoxTriStates(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckBoxTriStates(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        if(this.state != state) {
            this.state = state;

            if(this.clientListener != null) {
                this.clientListener.onCheckedChanged(this, this.isChecked());
            }

            if (NearbyController.currentLocation != null) {
                callback.filterByMarkerType(null, state, false, true);
            }
            updateBtn();
        }
    }

    @Override
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {

        // we never truly set the listener to the client implementation, instead we only hold
        // a reference to it and evoke it when needed.
        if(this.privateListener != listener) {
            this.clientListener = listener;
        }

        // always use our implementation
        super.setOnCheckedChangeListener(privateListener);
    }

    private void init() {
        state = UNKNOWN;
        updateBtn();
    }

    public void addAction() {
        setOnCheckedChangeListener(this.privateListener);
    }

    private void updateBtn() {
        int btnDrawable = R.drawable.ic_indeterminate_check_box_black_24dp;
        switch (state) {
            case UNKNOWN:
                btnDrawable = R.drawable.ic_indeterminate_check_box_black_24dp;
                break;
            case UNCHECKED:
                btnDrawable = R.drawable.ic_check_box_outline_blank_black_24dp;
                break;
            case CHECKED:
                btnDrawable = R.drawable.ic_check_box_black_24dp;
                break;
        }
        setButtonDrawable(btnDrawable);

    }
}