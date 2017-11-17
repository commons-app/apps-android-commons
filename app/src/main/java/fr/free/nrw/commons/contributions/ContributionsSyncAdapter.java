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

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.mwapi.LogEventResult;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;
import static fr.free.nrw.commons.contributions.Contribution.STATE_COMPLETED;
import static fr.free.nrw.commons.contributions.Contribution.Table.COLUMN_FILENAME;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI;

public class ContributionsSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String[] existsQuery = {COLUMN_FILENAME};
    private static final String existsSelection = COLUMN_FILENAME + " = ?";
    private static final ContentValues[] EMPTY = {};
    private static int COMMIT_THRESHOLD = 10;

    public ContributionsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    private int getLimit() {

        int limit = 500;
        Timber.d("Max number of uploads set to %d", limit);
        return limit; // FIXME: Parameterize!
    }

    private boolean fileExists(ContentProviderClient client, String filename) {
        if (filename == null) {
            return false;
        }
        Cursor cursor = null;
        try {
            cursor = client.query(BASE_URI,
                    existsQuery,
                    existsSelection,
                    new String[]{filename},
                    ""
            );
            return cursor != null && cursor.getCount() != 0;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String authority,
                              ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // This code is fraught with possibilities of race conditions, but lalalalala I can't hear you!
        String user = account.name;
        MediaWikiApi api = CommonsApplication.getInstance().getMWApi();
        SharedPreferences prefs = getContext().getSharedPreferences("prefs", MODE_PRIVATE);
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        Date curTime = new Date();
        LogEventResult result;
        Boolean done = false;
        String queryContinue = null;
        while (!done) {

            try {
                result = api.logEvents(user, lastModified, queryContinue, getLimit());
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
                Contribution contrib = new Contribution(null, thumbUrl, filename,
                        "", -1, dateUpdated, dateUpdated, user,
                        "", "");
                contrib.setState(STATE_COMPLETED);
                imageValues.add(contrib.toContentValues());

                if (imageValues.size() % COMMIT_THRESHOLD == 0) {
                    try {
                        contentProviderClient.bulkInsert(BASE_URI, imageValues.toArray(EMPTY));
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    imageValues.clear();
                }
            }

            if (imageValues.size() != 0) {
                try {
                    contentProviderClient.bulkInsert(BASE_URI, imageValues.toArray(EMPTY));
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
