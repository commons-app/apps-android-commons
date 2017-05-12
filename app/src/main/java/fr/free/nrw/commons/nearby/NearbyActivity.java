package fr.free.nrw.commons.nearby;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.theme.BaseActivity;

public class NearbyActivity extends BaseActivity {

    private LocationServiceManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        locationManager = new LocationServiceManager(this);
        locationManager.registerLocationManager();

        // Begin the transaction
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        NearbyListFragment fragment = new NearbyListFragment();
        ft.add(R.id.container, fragment);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nearby, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshView();
                return true;
            case R.id.action_map:
                Log.d("Nearby","map is clicked");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void refreshView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new NearbyListFragment()).commit();
    }

    public LocationServiceManager getLocationManager() {
        return locationManager;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.unregisterLocationManager();
    }
}
