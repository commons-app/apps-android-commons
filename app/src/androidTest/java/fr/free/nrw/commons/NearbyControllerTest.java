package fr.free.nrw.commons;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.Place;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
public class NearbyControllerTest {
    private Context instrumentationContext;

    @Before
    public void setup() {
        instrumentationContext = InstrumentationRegistry.getContext();
    }

    @Test public void testNullAttractions() {
        LatLng location = new LatLng(0, 0);

        List<NearbyBaseMarker> options =
            NearbyController.loadAttractionsFromLocationToBaseMarkerOptions(
                location,
                null,
                instrumentationContext
        );

        Assert.assertThat(options.size(), is(0));
    }

    @Test public void testEmptyList() {
        LatLng location = new LatLng(0, 0);
        List<Place> emptyList = new ArrayList<>();

        List<NearbyBaseMarker> options =
                NearbyController.loadAttractionsFromLocationToBaseMarkerOptions(
                        location,
                        emptyList,
                        instrumentationContext
                );

        Assert.assertThat(options.size(), is(0));
    }
}
