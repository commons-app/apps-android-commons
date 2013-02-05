package org.wikimedia.commons.contributions;

import android.content.*;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.accounts.Account;
import android.os.Bundle;
import org.apache.commons.codec.digest.DigestUtils;
import org.wikimedia.commons.CommonsApplication;
import org.mediawiki.api.*;
import org.wikimedia.commons.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContributionsSyncAdapter extends AbstractThreadedSyncAdapter {
    public ContributionsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    private int getLimit() {
        return 500; // FIXME: Parameterize!
    }
    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // This code is fraught with possibilities of race conditions, but lalalalala I can't hear you!
        String user = account.name;
        MWApi api = CommonsApplication.createMWApi();
        SharedPreferences prefs = this.getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        Date curTime = new Date();
        ApiResult result;
        try {
            MWApi.RequestBuilder builder = api.action("query")
                    .param("list", "logevents")
                    .param("leaction", "upload/upload")
                    .param("leprop", "title|timestamp")
                    .param("leuser", user)
                    .param("lelimit", getLimit());
            if(!TextUtils.isEmpty(lastModified)) {
                builder.param("leend", lastModified);
            }
            result = builder.get();
        } catch (IOException e) {
            throw new RuntimeException(e); // FIXME: Maybe something else?
        }
        Log.d("Commons", "Last modified at " + lastModified);

        ArrayList<ApiResult> uploads = result.getNodes("/api/query/logevents/item");
        Log.d("Commons", uploads.size() + " results!");
        ContentValues[] imageValues = new ContentValues[uploads.size()];
        for(int i=0; i < uploads.size(); i++) {
            ApiResult image = uploads.get(i);
            String filename = image.getString("@title");
            String thumbUrl = Utils.makeThumbBaseUrl(filename);
            Date dateUpdated = Utils.parseMWDate(image.getString("@timestamp"));
            Contribution contrib = new Contribution(null, thumbUrl, filename, "", -1, dateUpdated, dateUpdated, user, "");
            contrib.setState(Contribution.STATE_COMPLETED);
            imageValues[i] = contrib.toContentValues();
            Log.d("Commons", "For " + imageValues[i].toString());
        }

        try {
            contentProviderClient.bulkInsert(ContributionsContentProvider.BASE_URI, imageValues);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        prefs.edit().putString("lastSyncTimestamp", Utils.toMWDate(curTime)).apply();
        Log.d("Commons", "Oh hai, everyone! Look, a kitty!");


    }
}
