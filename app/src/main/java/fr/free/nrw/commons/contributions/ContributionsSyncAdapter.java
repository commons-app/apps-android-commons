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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.mwapi.LogEventResult;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

public class ContributionsSyncAdapter extends AbstractThreadedSyncAdapter {
    private static int COMMIT_THRESHOLD = 10;

    @Inject MediaWikiApi mwApi;

    public ContributionsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    private int getLimit() {

        int limit = 500;
        Timber.d("Max number of uploads set to %d", limit);
        return limit; // FIXME: Parameterize!
    }

    private static final String[] existsQuery = {Contribution.Table.COLUMN_FILENAME};
    private static final String existsSelection = Contribution.Table.COLUMN_FILENAME + " = ?";

    private boolean fileExists(ContentProviderClient client, String filename) {
        Cursor cursor = null;
        try {
            cursor = client.query(ContributionsContentProvider.BASE_URI,
                    existsQuery,
                    existsSelection,
                    new String[]{filename},
                    ""
            );
            return cursor.getCount() != 0;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        ((CommonsApplication) getContext().getApplicationContext()).injector().inject(this);

        // This code is fraught with possibilities of race conditions, but lalalalala I can't hear you!
        String user = account.name;
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        Date curTime = new Date();
        LogEventResult result;
        Boolean done = false;
        String queryContinue = null;
        while (!done) {

            try {
                result = mwApi.logEvents(user, lastModified, queryContinue, getLimit());
            } catch (IOException e) {
                // There isn't really much we can do, eh?
                // FIXME: Perhaps add EventLogging?
                syncResult.stats.numIoExceptions += 1; // Not sure if this does anything. Shitty docs
                Timber.d("Syncing failed due to %s", e);
                return;
            }
            Timber.d("Last modified at %s", lastModified);

            List<LogEventResult.LogEvent> logEvents = result.getLogEvents();
            Timber.d("%d results!", logEvents.size());
            ArrayList<ContentValues> imageValues = new ArrayList<>();
            for (LogEventResult.LogEvent image : logEvents) {
                if (image.isDeleted()) {
                    // means that this upload was deleted.
                    continue;
                }
                String filename = image.getFilename();
                if (fileExists(contentProviderClient, filename)) {
                    Timber.d("Skipping %s", filename);
                    continue;
                }
                String thumbUrl = Utils.makeThumbBaseUrl(filename);
                Date dateUpdated = image.getDateUpdated();
                Contribution contrib = new Contribution(null, thumbUrl, filename, "", -1, dateUpdated, dateUpdated, user, "", "");
                contrib.setState(Contribution.STATE_COMPLETED);
                imageValues.add(contrib.toContentValues());

                if (imageValues.size() % COMMIT_THRESHOLD == 0) {
                    try {
                        contentProviderClient.bulkInsert(ContributionsContentProvider.BASE_URI, imageValues.toArray(new ContentValues[]{}));
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    imageValues.clear();
                }
            }

            if (imageValues.size() != 0) {
                try {
                    contentProviderClient.bulkInsert(ContributionsContentProvider.BASE_URI, imageValues.toArray(new ContentValues[]{}));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }

            queryContinue = result.getQueryContinue();
            if (TextUtils.isEmpty(queryContinue)) {
                done = true;
            }
        }
        prefs.edit().putString("lastSyncTimestamp", Utils.toMWDate(curTime)).apply();
        Timber.d("Oh hai, everyone! Look, a kitty!");
    }
}
