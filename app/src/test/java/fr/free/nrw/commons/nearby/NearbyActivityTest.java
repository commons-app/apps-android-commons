package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import edu.emory.mathcs.backport.java.util.Collections;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.TestCommonsApplication;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class NearbyActivityTest {

    private static final LatLng ST_LOUIS_MO_LAT_LNG
            = new LatLng(38.627003, -90.199402, 0);
    private static final Place AIRPORT = new Place(
            "name", Place.Label.AIRPORT,
            "desc", null,
            new LatLng(38.6270, -90.1994, 0),
            null);

    @Mock
    private LocationServiceManager locationManager;

    @Mock
    private NearbyController nearbyController;

    @InjectMocks
    private NearbyActivity nearbyActivity;

    @Before
    public void setUp() throws Exception {
        // ensure waiting all threads to complete
        RxJavaPlugins.setIoSchedulerHandler(
                scheduler -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(
                scheduler -> Schedulers.trampoline());
        RxJavaPlugins.setNewThreadSchedulerHandler(
                scheduler -> Schedulers.trampoline());
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(
                scheduler -> Schedulers.trampoline());

        nearbyActivity = Robolectric.setupActivity(NearbyActivity.class);

        // replace methods and fields with mocks
        MockitoAnnotations.initMocks(this);
        when(locationManager.getLastLocation()).thenReturn(ST_LOUIS_MO_LAT_LNG);
        when(locationManager.isProviderEnabled()).thenReturn(true);
        when(nearbyController.loadAttractionsFromLocation(any(LatLng.class), any(Context.class)))
                .thenReturn(Collections.singletonList(AIRPORT));
    }

    @Test
    public void pressRefreshAndShowList() {
        MenuItem refresh = shadowOf(nearbyActivity).getOptionsMenu().findItem(R.id.action_refresh);
        nearbyActivity.onOptionsItemSelected(refresh);

        Fragment nearbyListFragment = nearbyActivity.getSupportFragmentManager()
                .findFragmentByTag("NearByListFragment");
        assertNotNull(nearbyListFragment);

        // one element (AIRPORT) exists in the list
        RecyclerView view = nearbyListFragment.getView().findViewById(R.id.listView);
        assertNotNull(view.findViewHolderForAdapterPosition(0));
        assertNull(view.findViewHolderForAdapterPosition(1));
    }

}