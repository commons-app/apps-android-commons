package fr.free.nrw.commons.nearby;

import android.app.Activity;
import android.support.v4.app.Fragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import dagger.android.AndroidInjector;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.TestCommonsApplication;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class NearbyActivityTest {

    private static final LatLng ST_LOUIS_MO_LAT_LNG = new LatLng(38.627003, -90.199402, 0);

    @Mock
    LocationServiceManager mockLocationManager;

    private ActivityController<NearbyActivity> activityController;
    private NearbyActivity nearbyActivity;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        AndroidInjector<Activity> injector = ((TestCommonsApplication) RuntimeEnvironment.application).activityInjector();
        Mockito.doNothing().when(injector).inject(isA(NearbyActivity.class));
        when(mockLocationManager.getLastLocation()).thenReturn(ST_LOUIS_MO_LAT_LNG);

        activityController = Robolectric.buildActivity(NearbyActivity.class);
        nearbyActivity = activityController.get();
        nearbyActivity.locationManager = mockLocationManager;
    }

    @Test
    public void activityLaunchesAndShowsList() {
        activityController.create().resume().visible();
        assertNotNull(nearbyActivity.getSupportFragmentManager().findFragmentByTag("NearbyListFragment"));
    }

}