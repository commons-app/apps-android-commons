package fr.free.nrw.commons.caching;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by misao on 05-Jan-16.
 */
public class CacheController {

    private Context context;
    private double decLongitude;
    private double decLatitude;
    private QuadTree quadTree;
    private List<String> categoryList;

    public CacheController(Context context, double decLongitude, double decLatitude) {
        this.context = context;
        this.decLongitude = decLongitude;
        this.decLatitude = decLatitude;
    }


    public void initQuadTree() {
        quadTree = new QuadTree(-180, -90, +180, +90);
        Log.d("Cache", "New QuadTree created");
        Log.d("Cache", "X (longitude) value: " + decLongitude + ", Y (latitude) value: " + decLatitude);
    }


    public void cacheCategory() {

        //TODO: Remove later, only setting categories manually for debugging
        categoryList = new ArrayList<String>();
        categoryList.add("UK");
        categoryList.add("US");
        quadTree.set(decLongitude, decLatitude, categoryList);
    }

    public void findCategory() {
        //TODO: Convert decLatitude and decLongitude to a range
        quadTree.searchWithin(final double xmin, final double ymin, final double xmax, final double ymax);
    }
}
