package fr.free.nrw.commons;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Test;

import fr.free.nrw.commons.location.LatLng;

public class LatLngTests {
    @Test public void testZeroZero() {
        LatLng place = new LatLng(0, 0);
        String prettyString = place.getPrettyCoordinateString();
        Assert.assertThat(prettyString, is("0.0 N, 0.0 E"));
    }

    @Test public void testAntipode() {
        LatLng place = new LatLng(0, 180);
        String prettyString = place.getPrettyCoordinateString();
        Assert.assertThat(prettyString, is("0.0 N, 180.0 W"));
    }

    @Test public void testNorthPole() {
        LatLng place = new LatLng(90, 0);
        String prettyString = place.getPrettyCoordinateString();
        Assert.assertThat(prettyString, is("90.0 N, 0.0 E"));
    }

    @Test public void testSouthPole() {
        LatLng place = new LatLng(-90, 0);
        String prettyString = place.getPrettyCoordinateString();
        Assert.assertThat(prettyString, is("90.0 S, 0.0 E"));
    }

    @Test public void testLargerNumbers() {
        LatLng place = new LatLng(120, 380);
        String prettyString = place.getPrettyCoordinateString();
        Assert.assertThat(prettyString, is("90.0 N, 20.0 E"));
    }

    @Test public void testNegativeNumbers() {
        LatLng place = new LatLng(-120, -30);
        String prettyString = place.getPrettyCoordinateString();
        Assert.assertThat(prettyString, is("90.0 S, 30.0 W"));
    }

    @Test public void testTooBigWestValue() {
        LatLng place = new LatLng(20, -190);
        String prettyString = place.getPrettyCoordinateString();
        Assert.assertThat(prettyString, is("20.0 N, 170.0 E"));
    }

    @Test public void testRounding() {
        LatLng place = new LatLng(0.1234567, -0.33333333);
        String prettyString = place.getPrettyCoordinateString();
        Assert.assertThat(prettyString, is("0.1235 N, 0.3333 W"));
    }

    @Test public void testRoundingAgain() {
        LatLng place = new LatLng(-0.000001, -0.999999);
        String prettyString = place.getPrettyCoordinateString();
        Assert.assertThat(prettyString, is("0.0 S, 1.0 W"));
    }
}
