package fr.free.nrw.commons.caching;

import android.content.Context;
import android.util.Log;

/**
 * Created by misao on 05-Jan-16.
 */
public class CacheController {

    private Context context;
    private String coords;

    public CacheController(Context context, String coords) {
        this.context = context;
        this.coords = coords;
    }

    public String getCoords() {
        Log.d("Cache", "Coords passed to cache: " + coords);
        return coords;
    }
}
