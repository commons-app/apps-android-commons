package fr.free.nrw.commons;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;

import static fr.free.nrw.commons.nearby.NearbyController.loadAttractionsFromLocationToBaseMarkerOptions;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class NearbyControllerTest {

    @Test
    public void testNullAttractions() {
        LatLng location = new LatLng(0, 0, 0);

        List<NearbyBaseMarker> options = loadAttractionsFromLocationToBaseMarkerOptions(
                location, null, RuntimeEnvironment.application);

        assertThat(options.size(), is(0));
    }

    @Test
    public void testEmptyList() {
        LatLng location = new LatLng(0, 0, 0);
        List<Place> emptyList = new ArrayList<>();

        List<NearbyBaseMarker> options = loadAttractionsFromLocationToBaseMarkerOptions(
                location, emptyList, RuntimeEnvironment.application);

        assertThat(options.size(), is(0));
    }
}
