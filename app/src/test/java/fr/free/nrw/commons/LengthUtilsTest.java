package fr.free.nrw.commons;

import static org.hamcrest.CoreMatchers.is;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.utils.LengthUtils;

import org.junit.Assert;
import org.junit.Test;

public class LengthUtilsTest {
    @Test public void testZeroDistance() {
        LatLng pointA = new LatLng(0, 0);
        LatLng pointB = new LatLng(0, 0);
        String distance = LengthUtils.formatDistanceBetween(pointA, pointB);
        Assert.assertThat(distance, is("0m"));
    }

    @Test public void testOneDegreeOnEquator() {
        LatLng pointA = new LatLng(0, 0);
        LatLng pointB = new LatLng(0, 1);
        String distance = LengthUtils.formatDistanceBetween(pointA, pointB);
        Assert.assertThat(distance, is("111.2km"));
    }

    @Test public void testOneDegreeFortyFiveDegrees() {
        LatLng pointA = new LatLng(45, 0);
        LatLng pointB = new LatLng(45, 1);
        String distance = LengthUtils.formatDistanceBetween(pointA, pointB);
        Assert.assertThat(distance, is("78.6km"));
    }

    @Test public void testOneDegreeSouthPole() {
        LatLng pointA = new LatLng(-90, 0);
        LatLng pointB = new LatLng(-90, 1);
        String distance = LengthUtils.formatDistanceBetween(pointA, pointB);
        Assert.assertThat(distance, is("0m"));
    }

    @Test public void testPoleToPole() {
        LatLng pointA = new LatLng(90, 0);
        LatLng pointB = new LatLng(-90, 0);
        String distance = LengthUtils.formatDistanceBetween(pointA, pointB);
        Assert.assertThat(distance, is("20,015.1km"));
    }
}
