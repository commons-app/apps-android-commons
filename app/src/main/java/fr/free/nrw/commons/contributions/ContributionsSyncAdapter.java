package fr.free.nrw.commons.contributions;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import org.mediawiki.api.ApiResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.MWApi;
import fr.free.nrw.commons.Utils;
import timber.log.Timber;

public class ContributionsSyncAdapter extends AbstractThreadedSyncAdapter {
    private static int COMMIT_THRESHOLD = 10;
    public ContributionsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    private int getLimit() {

        int limit = 500;
        Timber.d("Max number of uploads set to %d", limit);
        return limit; // FIXME: Parameterize!
    }

    private static final String[] existsQuery = { Contribution.Table.COLUMN_FILENAME };
    private static final String existsSelection = Contribution.Table.COLUMN_FILENAME + " = ?";
    private boolean fileExists(ContentProviderClient client, String filename) {
        Cursor cursor = null;
        try {
            cursor = client.query(ContributionsContentProvider.BASE_URI,
                    existsQuery,
                    existsSelection,
                    new String[] { filename },
                    ""
            );
            return cursor.getCount() != 0;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // This code is fraught with possibilities of race conditions, but lalalalala I can't hear you!
        String user = account.name;
        MWApi api = CommonsApplication.getInstance().getMWApi();
        SharedPreferences prefs = this.getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        Date curTime = new Date();
        ApiResult result;
        Boolean done = false;
        String queryContinue = null;
        while(!done) {

            try {
                MWApi.RequestBuilder builder = api.action("query")
                        .param("list", "logevents")
                        .param("letype", "upload")
                        .param("leprop", "title|timestamp")
                        .param("leuser", user)
                        .param("lelimit", getLimit());
                if(!TextUtils.isEmpty(lastModified)) {
                    builder.param("leend", lastModified);
                }
                if(!TextUtils.isEmpty(queryContinue)) {
                    builder.param("lestart", queryContinue);
                }
                result = builder.get();
            } catch (IOException e) {
                // There isn't really much we can do, eh?
                // FIXME: Perhaps add EventLogging?
                syncResult.stats.numIoExceptions += 1; // Not sure if this does anything. Shitty docs
                Timber.d("Syncing failed due to %s", e);
                return;
            }
            Timber.d("Last modified at %s", lastModified);

            ArrayList<ApiResult> uploads = result.getNodes("/api/query/logevents/item");
            Timber.d("%d results!", uploads.size());
            ArrayList<ContentValues> imageValues = new ArrayList<>();
            for(ApiResult image: uploads) {
                String filename = image.getString("@title");
                if(fileExists(contentProviderClient, filename)) {
                    Timber.d("Skipping %s", filename);
                    continue;
                }
                String thumbUrl = Utils.makeThumbBaseUrl(filename);
                Date dateUpdated = Utils.parseMWDate(image.getString("@timestamp"));
                Contribution contrib = new Contribution(null, thumbUrl, filename, "", -1, dateUpdated, dateUpdated, user, "", "");
                contrib.setState(Contribution.STATE_COMPLETED);
                imageValues.add(contrib.toContentValues());

                if(imageValues.size() % COMMIT_THRESHOLD == 0) {
                    try {
                        contentProviderClient.bulkInsert(ContributionsContentProvider.BASE_URI, imageValues.toArray(new ContentValues[]{}));
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    imageValues.clear();
                }
            }

            if(imageValues.size() != 0) {
                try {
                    contentProviderClient.bulkInsert(ContributionsContentProvider.BASE_URI, imageValues.toArray(new ContentValues[]{}));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            queryContinue = result.getString("/api/query-continue/logevents/@lestart");
            if(TextUtils.isEmpty(queryContinue)) {
                done = true;
            }
        }
        prefs.edit().putString("lastSyncTimestamp", Utils.toMWDate(curTime)).apply();
        Timber.d("Oh hai, everyone! Look, a kitty!");
    }
}
