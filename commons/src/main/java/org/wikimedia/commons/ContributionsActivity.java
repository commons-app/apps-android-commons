package org.wikimedia.commons;

import android.content.BroadcastReceiver;
import android.content.*;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.actionbarsherlock.app.SherlockActivity;
import org.wikimedia.commons.contributions.Contribution;

public class ContributionsActivity extends SherlockActivity  {

    private LocalBroadcastManager localBroadcastManager;

    private String[] broadcastsToReceive = {
            UploadService.INTENT_UPLOAD_STARTED,
            UploadService.INTENT_UPLOAD_QUEUED,
            UploadService.INTENT_UPLOAD_PROGRESS,
            UploadService.INTENT_UPLOAD_COMPLETE
    };

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Contribution contribution = (Contribution)intent.getParcelableExtra(UploadService.EXTRA_MEDIA);
            Log.d("Commons", "Completed " + intent.getAction() +" of " + contribution.getFilename());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        for(int i=0; i < broadcastsToReceive.length; i++) {
            localBroadcastManager.registerReceiver(messageReceiver, new IntentFilter(broadcastsToReceive[i]));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        for(int i=0; i < broadcastsToReceive.length; i++) {
            localBroadcastManager.unregisterReceiver(messageReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        setContentView(R.layout.activity_contributions);
    }
}
