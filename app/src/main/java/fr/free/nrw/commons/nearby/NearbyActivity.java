package fr.free.nrw.commons.nearby;

import android.os.Bundle;
import android.app.Activity;

import fr.free.nrw.commons.R;

public class NearbyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
