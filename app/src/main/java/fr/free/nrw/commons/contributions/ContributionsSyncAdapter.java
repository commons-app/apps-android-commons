package fr.free.nrw.commons.contributions;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import org.wikipedia.dataclient.mwapi.MwQueryLogEvent;
import org.wikipedia.dataclient.mwapi.MwQueryResult;
import org.wikipedia.util.DateUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.LogEventResult;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.UserClient;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.Contribution.STATE_COMPLETED;
import static fr.free.nrw.commons.contributions.ContributionDao.Table.COLUMN_FILENAME;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI;

@SuppressWarnings("WeakerAccess")
public class ContributionsSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String[] existsQuery = {COLUMN_FILENAME};
    private static final String existsSelection = COLUMN_FILENAME + " = ?";
    private static final ContentValues[] EMPTY = {};
    private static int COMMIT_THRESHOLD = 10;

    // Arbitrary limit to cap the number of contributions to ever load. This is a maximum built
    // into the app, rather than the user's setting. Also see Github issue #52.
    public static final int ABSOLUTE_CONTRIBUTIONS_LOAD_LIMIT = 500;

    @Inject
    UserClient userClient;
    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;

    public ContributionsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
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

    @SuppressLint("CheckResult")
    @Override
    public void onPerformSync(Account account, Bundle bundle, String authority,
                              ContentProviderClient contentProviderClient, SyncResult syncResult) {
        ApplicationlessInjection
                .getInstance(getContext()
                        .getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);
        // This code is fraught with possibilities of race conditions, but lalalalala I can't hear you!
        String user = account.name;
        String lastSynced = defaultKvStore.getString("lastSyncTimestamp", "");
        Date curTime = new Date();
        ContributionDao contributionDao = new ContributionDao(() -> contentProviderClient);
        Timber.d("Last modified at %s", lastSynced);
        userClient.logEvents(user)
                .doOnNext(mwQueryLogEvent->Timber.d("Received image %s", mwQueryLogEvent.title() ))
                .filter(mwQueryLogEvent -> !mwQueryLogEvent.isDeleted())
                .filter(mwQueryLogEvent -> !fileExists(contentProviderClient, mwQueryLogEvent.title()))
                .doOnNext(mwQueryLogEvent->Timber.d("Image %s passed filters", mwQueryLogEvent.title() ))
                .map(image -> new Contribution(null, null, image.title(),
                        "", -1, image.date(), image.date(), user,
                        "", "", STATE_COMPLETED))
                .map(contributionDao::toContentValues)
                .buffer(10)
                .subscribe(imageValues->contentProviderClient.bulkInsert(BASE_URI, imageValues.toArray(EMPTY)));
        defaultKvStore.putString("lastSyncTimestamp", DateUtil.iso8601DateFormat(curTime));
        Timber.d("Oh hai, everyone! Look, a kitty!");
    }
}
