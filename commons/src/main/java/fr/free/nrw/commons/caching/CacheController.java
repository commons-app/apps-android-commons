package fr.free.nrw.commons.caching;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CacheController {

    private Context context;
    private double x;
    private double y;
    private QuadTree quadTree;
    private List<String> categoryList;
    private Point[] pointsFound;

    public CacheController(Context context, double decLongitude, double decLatitude) {
        this.context = context;
        x = decLongitude;
        y = decLatitude;
    }


    public void initQuadTree() {
        quadTree = new QuadTree(-180, -90, +180, +90);
        Log.d("Cache", "New QuadTree created");
        Log.d("Cache", "X (longitude) value: " + x + ", Y (latitude) value: " + y);
    }


    public void cacheCategory() {

        //TODO: Remove later, only setting categories manually for debugging
        categoryList = new ArrayList<String>();
        categoryList.add("UK");
        categoryList.add("US");
        quadTree.set(x, y, categoryList);
    }

    public void findCategory() {
        //TODO: Convert decLatitude and decLongitude to a range with proper formula, for testing just use 10
        pointsFound = quadTree.searchWithin(x-10, y-10, x+10, y+10);
        Log.d("Cache", "Points found: " + pointsFound.toString());
        double x;
        Object cat = null;
        //TODO: This does not truly iterate, just for testing. In future probably need to store results in Array
        for (Point point: pointsFound) {
            x = point.getX();
            cat = point.getValue();
        }
        Log.d("Cache", "Categories found: " + cat.toString());

    }
}
