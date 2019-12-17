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

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
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
        try (Cursor cursor = client.query(BASE_URI,
                existsQuery,
                existsSelection,
                new String[]{filename},
                ""
        )) {
            return cursor != null && cursor.getCount() != 0;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
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
        // This code is(was?) fraught with possibilities of race conditions, but lalalalala I can't hear you!
        String user = account.name;
        ContributionDao contributionDao = new ContributionDao(() -> contentProviderClient);
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
        Timber.d("Oh hai, everyone! Look, a kitty!");
    }
}
