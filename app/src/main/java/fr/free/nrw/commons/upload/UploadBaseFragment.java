package fr.free.nrw.commons.upload;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class UploadBaseFragment extends Fragment {

    public int indexInViewFlipper;
    public Callback callback;
    public int totalNumberOfSteps = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setIndexInViewFlipper(int indexInViewFlipper) {
        this.indexInViewFlipper = indexInViewFlipper;
    }

    public void setTotalNumberOfSteps(int totalNumberOfSteps) {
        this.totalNumberOfSteps = totalNumberOfSteps;
    }

    public interface Callback {

        void onNextButtonClicked(int index);

        void onPreviousButtonClicked(int index);

        void showProgress(boolean shouldShow);
    }
}
