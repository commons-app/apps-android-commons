package fr.free.nrw.commons.caching;

import com.github.varunpant.quadtree.Point;
import com.github.varunpant.quadtree.QuadTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.free.nrw.commons.upload.MwVolleyApi;
import timber.log.Timber;

public class CacheController {

    private double x, y;
    private QuadTree<List<String>> quadTree;
    private double xMinus, xPlus, yMinus, yPlus;

    private static final int EARTH_RADIUS = 6378137;

    public CacheController() {
        quadTree = new QuadTree<>(-180, -90, +180, +90);
    }

    public void setQtPoint(double decLongitude, double decLatitude) {
        x = decLongitude;
        y = decLatitude;
        Timber.d("New QuadTree created");
        Timber.d("X (longitude) value: %f, Y (latitude) value: %f", x, y);
    }

    public void cacheCategory() {
        List<String> pointCatList = new ArrayList<>();
        if (MwVolleyApi.GpsCatExists.getGpsCatExists()) {
            pointCatList.addAll(MwVolleyApi.getGpsCat());
            Timber.d("Categories being cached: %s", pointCatList);
        } else {
            Timber.d("No categories found, so no categories cached");
        }
        quadTree.set(x, y, pointCatList);
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
    public void convertCoordRange() {
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
