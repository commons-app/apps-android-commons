package fr.free.nrw.commons.upload;

import android.os.Bundle;

import androidx.annotation.Nullable;

import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;

/**
 * The base fragment of the fragments in upload
 */
public class UploadBaseFragment extends CommonsDaggerSupportFragment {

    public Callback callback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {

        void onNextButtonClicked(int index);

        void onPreviousButtonClicked(int index);

        void showProgress(boolean shouldShow);

        int getIndexInViewFlipper(UploadBaseFragment fragment);

        int getTotalNumberOfSteps();

    }
}
