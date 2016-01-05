package fr.free.nrw.commons.caching;

import android.content.Context;
import android.util.Log;

/**
 * Created by misao on 05-Jan-16.
 */
public class CacheController {

    private Context context;
    private double decLongitude;
    private double decLatitude;
    private QuadTree quadTree;
    private String category = "test";

    public CacheController(Context context, double decLongitude, double decLatitude) {
        this.context = context;
        this.decLongitude = decLongitude;
        this.decLatitude = decLatitude;

    }


    public void callQuadTree() {
        quadTree = new QuadTree(-180, -90, +180, +90);
        Log.d("Cache", "New QuadTree created");
        Log.d("Cache", "X (longitude) value: " + decLongitude + ", Y (latitude) value: " + decLatitude);
        quadTree.set(decLongitude, decLatitude, category);
    }
}
