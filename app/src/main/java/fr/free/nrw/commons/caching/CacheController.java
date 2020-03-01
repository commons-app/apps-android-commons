package fr.free.nrw.commons.caching;

import com.github.varunpant.quadtree.Point;
import com.github.varunpant.quadtree.QuadTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class CacheController {

    private final QuadTree<List<String>> quadTree;
    private double x, y;
    private double xMinus, xPlus, yMinus, yPlus;

    private static final int EARTH_RADIUS = 6378137;

    @Inject
    public CacheController(QuadTree quadTree) {
        this.quadTree = quadTree;
    }

    public void setQtPoint(double decLongitude, double decLatitude) {
        x = decLongitude;
        y = decLatitude;
        Timber.d("New QuadTree created");
        Timber.d("X (longitude) value: %f, Y (latitude) value: %f", x, y);
    }

    public List<String> findCategory() {
        Point<List<String>>[] pointsFound;
        //Convert decLatitude and decLongitude to a coordinate offset range
        convertCoordRange();
        pointsFound = quadTree.searchWithin(xMinus, yMinus, xPlus, yPlus);
        List<String> displayCatList = new ArrayList<>();
        Timber.d("Points found in quadtree: %s", Arrays.toString(pointsFound));

        if (pointsFound.length != 0) {
            Timber.d("Entering for loop");

            for (Point<List<String>> point : pointsFound) {
                Timber.d("Nearby point: %s", point);
                displayCatList = point.getValue();
                Timber.d("Nearby cat: %s", point.getValue());
            }

            Timber.d("Categories found in cache: %s", displayCatList);
        } else {
            Timber.d("No categories found in cache");
        }
        return displayCatList;
    }

    //Based on algorithm at http://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
    private void convertCoordRange() {
        //Position, decimal degrees
        double lat = y;
        double lon = x;

        //offsets in meters
        double offset = 100;

        //Coordinate offsets in radians
        double dLat = offset / EARTH_RADIUS;
        double dLon = offset / (EARTH_RADIUS * Math.cos(Math.PI * lat / 180));

        //OffsetPosition, decimal degrees
        yPlus  = lat + dLat * 180 / Math.PI;
        yMinus = lat - dLat * 180 / Math.PI;
        xPlus  = lon + dLon * 180 / Math.PI;
        xMinus = lon - dLon * 180 / Math.PI;
        Timber.d("Search within: xMinus=%s, yMinus=%s, xPlus=%s, yPlus=%s",
                xMinus, yMinus, xPlus, yPlus);
    }
}
