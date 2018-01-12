package fr.free.nrw.commons.di;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;

public abstract class FixedDaggerFragment extends Fragment implements HasSupportFragmentInjector {

    @Inject
    DispatchingAndroidInjector<Fragment> childFragmentInjector;

    @Override
    public void onAttach(Context context) {
        inject();
        super.onAttach(context);
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return childFragmentInjector;
    }


    public void inject() {
        HasSupportFragmentInjector hasSupportFragmentInjector = findHasFragmentInjector();

        AndroidInjector<Fragment> fragmentInjector = hasSupportFragmentInjector.supportFragmentInjector();

        if (fragmentInjector == null) {
            throw new NullPointerException(String.format("%s.supportFragmentInjector() returned null", hasSupportFragmentInjector.getClass().getCanonicalName()));
        }

        fragmentInjector.inject(this);
    }

    private HasSupportFragmentInjector findHasFragmentInjector() {
        Fragment parentFragment = this;

        while ((parentFragment = parentFragment.getParentFragment()) != null) {
            if (parentFragment instanceof HasSupportFragmentInjector) {
                return (HasSupportFragmentInjector) parentFragment;
            }
        }

        Activity activity = getActivity();

        if (activity instanceof HasSupportFragmentInjector) {
            return (HasSupportFragmentInjector) activity;
        }

        ApplicationlessInjection injection = ApplicationlessInjection.getInstance(activity.getApplicationContext());
        if (injection != null) {
            return injection;
        }

        throw new IllegalArgumentException(String.format("No injector was found for %s", getClass().getCanonicalName()));
    }

}