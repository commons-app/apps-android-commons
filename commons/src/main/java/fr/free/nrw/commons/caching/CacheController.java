package fr.free.nrw.commons.caching;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.upload.MwVolleyApi;

public class CacheController {

    private double x, y;
    private QuadTree quadTree;
    private Point[] pointsFound;
    private double xMinus, xPlus, yMinus, yPlus;

    public CacheController() {
        quadTree = new QuadTree(-180, -90, +180, +90);
    }

    public void setQtPoint(double decLongitude, double decLatitude) {
        x = decLongitude;
        y = decLatitude;
        Log.d("Cache", "New QuadTree created");
        Log.d("Cache", "X (longitude) value: " + x + ", Y (latitude) value: " + y);
    }

    public void cacheCategory() {

        List<String> pointCatList = new ArrayList<String>();
        if (MwVolleyApi.GpsCatExists.getGpsCatExists() == true) {
             pointCatList.addAll(MwVolleyApi.getGpsCat());
            Log.d("Cache", "Categories being cached: " + pointCatList);
        } else {
            Log.d("Cache", "No categories found, so no categories cached");
        }
        quadTree.set(x, y, pointCatList);
    }

    public ArrayList findCategory() {

        //Convert decLatitude and decLongitude to a coordinate offset range
        convertCoordRange();
        pointsFound = quadTree.searchWithin(xMinus, yMinus, xPlus, yPlus);
        ArrayList displayCatList = new ArrayList();
        Log.d("Cache", "Points found in quadtree: " + pointsFound);

        ArrayList<String> flatCatList = null;
        //TODO: Make this return a proper flat array
        if (pointsFound.length != 0) {

            Log.d("Cache", "Entering for loop");
            int index = 0;
            for (Point point : pointsFound) {
                Log.d("Cache", "Nearby point: " + point.toString());
                Object cat = point.getValue();
                Log.d("Cache", "Nearby cat: " + cat);
                displayCatList.add(index, cat);
                index++;
            }
            //FIXME: temporary, can't figure out why for loop always only accesses 1 point
             flatCatList = ((ArrayList<String>)displayCatList.get(0));
            Log.d("Cache", "Categories found in cache: " + flatCatList.toString());
        } else {
            Log.d("Cache", "No categories found in cache");
        }
        return flatCatList;
    }



    public void convertCoordRange() {
        //Position, decimal degrees
        double lat = y;
        double lon = x;

        //Earth’s radius, sphere
        double radius=6378137;
        //offsets in meters
        double offset = 100;

        //Coordinate offsets in radians
        double dLat = offset/radius;
        double dLon = offset/(radius*Math.cos(Math.PI*lat/180));

        //OffsetPosition, decimal degrees
        yPlus = lat + dLat * 180/Math.PI;
        yMinus = lat - dLat * 180/Math.PI;
        xPlus = lon + dLon * 180/Math.PI;
        xMinus = lon - dLon * 180/Math.PI;
        Log.d("Cache", "Search within: xMinus=" + xMinus + ", yMinus=" + yMinus + ", xPlus=" + xPlus + ", yPlus=" + yPlus);
    }
}
