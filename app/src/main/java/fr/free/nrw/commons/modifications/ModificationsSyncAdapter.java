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

import fr.free.nrw.commons.MWApi;

import java.io.IOException;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;
import fr.free.nrw.commons.libs.mediawiki_api.ApiResult;
import timber.log.Timber;

public class ModificationsSyncAdapter extends AbstractThreadedSyncAdapter {

    public ModificationsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // This code is fraught with possibilities of race conditions, but lalalalala I can't hear you!

        Cursor allModifications;
        try {
            allModifications = contentProviderClient.query(ModificationsContentProvider.BASE_URI, null, null, null, null);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        // Exit early if nothing to do
        if(allModifications == null || allModifications.getCount() == 0) {
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

        if(Utils.isNullOrWhiteSpace(authCookie)) {
            Timber.d("Could not authenticate :(");
            return;
        }

        MWApi api = CommonsApplication.getInstance().getMWApi();
        api.setAuthCookie(authCookie);
        String editToken;

        ApiResult requestResult, responseResult;
        try {
            editToken = api.getEditToken();
        } catch (IOException e) {
            Timber.d("Can not retreive edit token!");
            return;
        }

        allModifications.moveToFirst();

        Timber.d("Found %d modifications to execute", allModifications.getCount());

        ContentProviderClient contributionsClient = null;
        try {
            contributionsClient = getContext().getContentResolver().acquireContentProviderClient(ContributionsContentProvider.AUTHORITY);

            while(!allModifications.isAfterLast()) {
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
                contrib = Contribution.fromCursor(contributionCursor);

                if(contrib.getState() == Contribution.STATE_COMPLETED) {

                    try {
                        requestResult = api.action("query")
                                .param("prop", "revisions")
                                .param("rvprop", "timestamp|content")
                                .param("titles", contrib.getFilename())
                                .prepareHttpRequestBuilder("GET")
                                .request();;
                    } catch (IOException e) {
                        Timber.d("Network fuckup on modifications sync!");
                        continue;
                    }

                    Timber.d("Page content is %s", Utils.getStringFromDOM(requestResult.getDocument()));
                    String pageContent = requestResult.getString("/api/query/pages/page/revisions/rev");
                    String processedPageContent = sequence.executeModifications(contrib.getFilename(),  pageContent);

                    try {
                        responseResult = api.action("edit")
                                .param("title", contrib.getFilename())
                                .param("token", editToken)
                                .param("text", processedPageContent)
                                .param("summary", sequence.getEditSummary())
                                .prepareHttpRequestBuilder("POST")
                                .request();
                    } catch (IOException e) {
                        Timber.d("Network fuckup on modifications sync!");
                        continue;
                    }

                    Timber.d("Response is %s", Utils.getStringFromDOM(responseResult.getDocument()));

                    String result = responseResult.getString("/api/edit/@result");
                    if(!result.equals("Success")) {
                        // FIXME: Log this somewhere else
                        Timber.d("Non success result! %s", result);
                    } else {
                        sequence.delete();
                    }
                }
                allModifications.moveToNext();
            }
        } finally {
            if(contributionsClient != null) {
                contributionsClient.release();
            }
        }
    }
}
