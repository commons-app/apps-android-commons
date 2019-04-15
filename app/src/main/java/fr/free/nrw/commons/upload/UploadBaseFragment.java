package fr.free.nrw.commons.upload;

import android.support.v4.app.Fragment;

public class UploadBaseFragment extends Fragment {

    public int indexInViewFlipper;
    public Callback callback;
    public int totalNumberOfSteps = 0;

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

        int getMarginTop(int index);
    }
}
