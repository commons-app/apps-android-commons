package fr.free.nrw.commons.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.NearbyActivity;
import timber.log.Timber;

import static fr.free.nrw.commons.nearby.NearbyActivity.currentNearbyActivity;

/**
 * Created by balakrishnan on 22/3/18.
 */

public class NetworkStateChangeReceiver extends BroadcastReceiver{

    String callingActivity;
    public NetworkStateChangeReceiver(){}

    public NetworkStateChangeReceiver(String callingActivity) {
        this.callingActivity=callingActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Timber.d("Network connection :" + NetworkUtils.haveNetworkConnection(context));

        if(callingActivity!=null && callingActivity.equals("NearbyActivity") && currentNearbyActivity!=null) {
            if (NetworkUtils.haveNetworkConnection(context)) {
                //Internet available
                //Manually triggering onLocationChangedSignificantly to refresh
                ViewUtil.showSnackbar(currentNearbyActivity.findViewById(R.id.container), R.string.internet_established);
                currentNearbyActivity.onLocationChangedSignificantly(null);
            }
            else {
                //No internet available
                ViewUtil.showSnackbar(currentNearbyActivity.findViewById(R.id.container), R.string.no_internet);
                currentNearbyActivity.hideProgressBar();
            }
        }
        else if(callingActivity.equals("contributionsActivity")){
            //Check internet availability for contributions activity
        }
    }
}
