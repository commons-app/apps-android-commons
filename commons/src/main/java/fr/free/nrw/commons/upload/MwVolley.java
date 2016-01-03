package fr.free.nrw.commons.upload;

import android.content.Context;
import com.android.volley.RequestQueue;


public class MwVolley {

    public MwVolley(Context context) {

        //Instantiate RequestQueue with Application context
        RequestQueue queue = VolleyRequestQueue.getInstance(this.getApplicationContext()).getRequestQueue();

    }


}
