package fr.free.nrw.commons;

import android.app.Activity;

import com.squareup.leakcanary.RefWatcher;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import dagger.android.AndroidInjector;
import fr.free.nrw.commons.nearby.NearbyPlaces;

public class TestCommonsApplication extends CommonsApplication {
    @Mock
    private AndroidInjector<Activity> mockInjector;
    @Mock
    private NearbyPlaces mockNearbyPlaces;

    @Override
    public void onCreate() {
        super.onCreate();
        MockitoAnnotations.initMocks(this);
    }

    @Override
    protected RefWatcher setupLeakCanary() {
        // No leakcanary in unit tests.
        return RefWatcher.DISABLED;
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return mockInjector;
    }

    @Override
    public synchronized NearbyPlaces getNearbyPlaces() {
        return mockNearbyPlaces;
    }
}
