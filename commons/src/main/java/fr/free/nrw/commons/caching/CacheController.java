package fr.free.nrw.commons.caching;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.upload.MwVolleyApi;

public class CacheController {

    private Context context;
    private double x;
    private double y;
    private QuadTree quadTree;
    private List<String> categoryList;
    private Point[] pointsFound;

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

        if (MwVolleyApi.GpsCatExists.getGpsCatExists() == true) {
            categoryList = new ArrayList<String>(MwVolleyApi.getGpsCat());
            Log.d("Cache", "Categories being cached: " + categoryList);
        } else {
            Log.d("Cache", "No categories found, so no categories cached");
        }

        //categoryList = new ArrayList<String>();
        //categoryList.add("UK");
        //categoryList.add("US");
        quadTree.set(x, y, categoryList);
    }

    public void findCategory() {

        //TODO: Convert decLatitude and decLongitude to a range with proper formula, for testing just use 10
        pointsFound = quadTree.searchWithin(x-10, y-10, x+10, y+10);
        ArrayList catList = new ArrayList();
        Log.d("Cache", "Points found in quadtree: " + pointsFound);

        if (pointsFound.length != 0) {

            Log.d("Cache", "Entering for loop");
            int index = 0;
            for (Point point : pointsFound) {
                Log.d("Cache", "Nearby point: " + point.toString());
                Object cat = point.getValue();
                Log.d("Cache", "Nearby cat: " + cat);
                catList.add(index, cat);
                index++;
            }

            Log.d("Cache", "Categories found in cache: " + catList.toString());
        } else {
            Log.d("Cache", "No categories found in cache");
        }

    }
}
