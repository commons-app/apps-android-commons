package org.wikimedia.commons.modifications;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import org.wikimedia.commons.HandlerService;
import org.wikimedia.commons.R;
import org.wikimedia.commons.UploadService;

public class PostUploadActivity extends Activity {
    public static String EXTRA_MEDIA_URI = "org.wikimedia.commons.modifications.PostUploadActivity.mediauri";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_upload);
        Uri mediaUri = getIntent().getParcelableExtra(EXTRA_MEDIA_URI);
        ModifierSequence testSequence = new ModifierSequence(mediaUri);
        testSequence.queueModifier(new CategoryModifier("Hello, World!"));
        testSequence.setContentProviderClient(getContentResolver().acquireContentProviderClient(ModificationsContentProvider.AUTHORITY));
        testSequence.save();

    }
}