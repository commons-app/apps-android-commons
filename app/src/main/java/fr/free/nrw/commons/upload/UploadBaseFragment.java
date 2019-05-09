package fr.free.nrw.commons.upload;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment;

/**
 * The base fragment of the fragments in upload
 */
public class UploadBaseFragment extends Fragment {

    public Callback callback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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
