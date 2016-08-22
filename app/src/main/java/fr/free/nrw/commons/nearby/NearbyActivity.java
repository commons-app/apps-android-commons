package fr.free.nrw.commons.nearby;

import android.app.ListActivity;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

import fr.free.nrw.commons.R;

public class NearbyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    //TODO: Get user's location

}
