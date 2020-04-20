package org.wikipedia.nearby;

import android.location.Location;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.NearbyPage;
import org.wikipedia.page.PageTitle;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for Nearby related classes. Probably should refactor this into a model class.
 */
@SuppressWarnings("checkstyle:magicnumber") @RunWith(RobolectricTestRunner.class)
public class NearbyUnitTest {
    private static WikiSite TEST_WIKI_SITE = new WikiSite(Service.WIKIPEDIA_URL);
    /** dist(origin, point a) */
    private static final int A = 111_319;

    private Location nextLocation;
    private List<NearbyPage> nearbyPages;

    @Before
    public void setUp() {
        nextLocation = new Location("current");
        nextLocation.setLatitude(0.0d);
        nextLocation.setLongitude(0.0d);
        nearbyPages = new LinkedList<>();
        nearbyPages.add(constructNearbyPage("c", 0.0, 3.0));
        nearbyPages.add(constructNearbyPage("b", 0.0, 2.0));
        nearbyPages.add(constructNearbyPage("a", 0.0, 1.0));
    }

    @Test public void testSort() {
        calcDistances(nearbyPages);
        Collections.sort(nearbyPages, new NearbyDistanceComparator());
        assertThat("a", is(nearbyPages.get(0).getTitle().getDisplayText()));
        assertThat("b", is(nearbyPages.get(1).getTitle().getDisplayText()));
        assertThat("c", is(nearbyPages.get(2).getTitle().getDisplayText()));
    }

    @Test public void testSortWithNullLocations() {
        final Location location = null;
        nearbyPages.add(new NearbyPage(new PageTitle("d", TEST_WIKI_SITE), location));
        nearbyPages.add(new NearbyPage(new PageTitle("e", TEST_WIKI_SITE), location));
        calcDistances(nearbyPages);
        Collections.sort(nearbyPages, new NearbyDistanceComparator());
        assertThat("a", is(nearbyPages.get(0).getTitle().getDisplayText()));
        assertThat("b", is(nearbyPages.get(1).getTitle().getDisplayText()));
        assertThat("c", is(nearbyPages.get(2).getTitle().getDisplayText()));
        // the two null location values come last but in the same order as from the original list:
        assertThat("d", is(nearbyPages.get(3).getTitle().getDisplayText()));
        assertThat("e", is(nearbyPages.get(4).getTitle().getDisplayText()));
    }

    @Test public void testCompare() {
        final Location location = null;
        NearbyPage nullLocPage = new NearbyPage(new PageTitle("nowhere", TEST_WIKI_SITE), location);

        calcDistances(nearbyPages);
        nullLocPage.setDistance(getDistance(nullLocPage.getLocation()));
        assertThat(Integer.MAX_VALUE, is(nullLocPage.getDistance()));

        NearbyDistanceComparator comp = new NearbyDistanceComparator();
        assertThat(A, is(comp.compare(nearbyPages.get(1), nearbyPages.get(2))));
        assertThat(-1 * A, is(comp.compare(nearbyPages.get(2), nearbyPages.get(1))));
        assertThat(Integer.MAX_VALUE - A, is(comp.compare(nullLocPage, nearbyPages.get(2))));
        assertThat((Integer.MIN_VALUE + 1) + A, is(comp.compare(nearbyPages.get(2), nullLocPage))); // - (max - a)
        assertThat(0, is(comp.compare(nullLocPage, nullLocPage)));
    }

    private class NearbyDistanceComparator implements Comparator<NearbyPage> {
        @Override
        public int compare(NearbyPage a, NearbyPage b) {
            return a.getDistance() - b.getDistance();
        }
    }

    //
    // UGLY: copy of production code
    //

    /**
     * Calculates the distances from the origin to the given pages.
     * This method should be called before sorting.
     */
    private void calcDistances(List<NearbyPage> pages) {
        for (NearbyPage page : pages) {
            page.setDistance(getDistance(page.getLocation()));
        }
    }

    private int getDistance(Location otherLocation) {
        if (otherLocation == null) {
            return Integer.MAX_VALUE;
        } else {
            return (int) nextLocation.distanceTo(otherLocation);
        }
    }

    private NearbyPage constructNearbyPage(@NonNull String title, double lat, double lon) {
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        return new NearbyPage(new PageTitle(title, TEST_WIKI_SITE), location);
    }
}
