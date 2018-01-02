package fr.free.nrw.commons.modifications;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;

import java.io.IOException;

import javax.inject.Inject;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionDao;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

public class ModificationsSyncAdapter extends AbstractThreadedSyncAdapter {

    @Inject MediaWikiApi mwApi;

    public ModificationsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // This code is fraught with possibilities of race conditions, but lalalalala I can't hear you!
        ((CommonsApplication)getContext().getApplicationContext()).injector().inject(this);

        Cursor allModifications;
        try {
            allModifications = contentProviderClient.query(ModificationsContentProvider.BASE_URI, null, null, null, null);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        // Exit early if nothing to do
        if (allModifications == null || allModifications.getCount() == 0) {
            Timber.d("No modifications to perform");
            return;
        }

        String authCookie;
        try {
            authCookie = AccountManager.get(getContext()).blockingGetAuthToken(account, "", false);
        } catch (OperationCanceledException | AuthenticatorException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Timber.d("Could not authenticate :(");
            return;
        }

        if (isNullOrWhiteSpace(authCookie)) {
            Timber.d("Could not authenticate :(");
            return;
        }

        mwApi.setAuthCookie(authCookie);
        String editToken;

        try {
            editToken = mwApi.getEditToken();
        } catch (IOException e) {
            Timber.d("Can not retreive edit token!");
            return;
        }

        allModifications.moveToFirst();

        Timber.d("Found %d modifications to execute", allModifications.getCount());

        ContentProviderClient contributionsClient = null;
        try {
            contributionsClient = getContext().getContentResolver().acquireContentProviderClient(ContributionsContentProvider.AUTHORITY);

            while (!allModifications.isAfterLast()) {
                ModifierSequence sequence = ModifierSequence.fromCursor(allModifications);
                sequence.setContentProviderClient(contentProviderClient);
                Contribution contrib;

                Cursor contributionCursor;
                try {
                    contributionCursor = contributionsClient.query(sequence.getMediaUri(), null, null, null, null);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
                contributionCursor.moveToFirst();
                contrib = ContributionDao.fromCursor(contributionCursor);

                if (contrib.getState() == Contribution.STATE_COMPLETED) {
                    String pageContent;
                    try {
                        pageContent = mwApi.revisionsByFilename(contrib.getFilename());
                    } catch (IOException e) {
                        Timber.d("Network fuckup on modifications sync!");
                        continue;
                    }

                    Timber.d("Page content is %s", pageContent);
                    String processedPageContent = sequence.executeModifications(contrib.getFilename(), pageContent);

                    String editResult;
                    try {
                        editResult = mwApi.edit(editToken, processedPageContent, contrib.getFilename(), sequence.getEditSummary());
                    } catch (IOException e) {
                        Timber.d("Network fuckup on modifications sync!");
                        continue;
                    }

                    Timber.d("Response is %s", editResult);

                    if (!editResult.equals("Success")) {
                        // FIXME: Log this somewhere else
                        Timber.d("Non success result! %s", editResult);
                    } else {
                        sequence.delete();
                    }
                }
                allModifications.moveToNext();
            }
        } finally {
            if (contributionsClient != null) {
                contributionsClient.release();
            }
        }
    }

    private boolean isNullOrWhiteSpace(String value) {
        return value == null || value.trim().isEmpty();
    }
}
