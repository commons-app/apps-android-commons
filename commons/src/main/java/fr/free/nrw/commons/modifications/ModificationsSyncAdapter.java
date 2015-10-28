package fr.free.nrw.commons.modifications;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.*;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;
import android.accounts.Account;
import android.os.Bundle;

import java.io.*;

import fr.free.nrw.commons.contributions.Contribution;
import org.mediawiki.api.*;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;


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
            Log.d("Commons", "No modifications to perform");
            return;
        }

        String authCookie;
        try {
             authCookie = AccountManager.get(getContext()).blockingGetAuthToken(account, "", false);
        } catch (OperationCanceledException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.d("Commons", "Could not authenticate :(");
            return;
        } catch (AuthenticatorException e) {
            throw new RuntimeException(e);
        }

        MWApi api = CommonsApplication.createMWApi();
        api.setAuthCookie(authCookie);
        String editToken;

        ApiResult requestResult, responseResult;
        try {
            editToken = api.getEditToken();
        } catch (IOException e) {
            Log.d("Commons", "Can not retreive edit token!");
            return;
        }


        allModifications.moveToFirst();

        Log.d("Commons", "Found " + allModifications.getCount() + " modifications to execute");

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
                                .get();
                    } catch (IOException e) {
                        Log.d("Commons", "Network fuckup on modifications sync!");
                        continue;
                    }

                    Log.d("Commons", "Page content is " + Utils.getStringFromDOM(requestResult.getDocument()));
                    String pageContent = requestResult.getString("/api/query/pages/page/revisions/rev");
                    String processedPageContent = sequence.executeModifications(contrib.getFilename(),  pageContent);

                    try {
                        responseResult = api.action("edit")
                                .param("title", contrib.getFilename())
                                .param("token", editToken)
                                .param("text", processedPageContent)
                                .param("summary", sequence.getEditSummary())
                                .post();
                    } catch (IOException e) {
                        Log.d("Commons", "Network fuckup on modifications sync!");
                        continue;
                    }

                    Log.d("Commons", "Response is" + Utils.getStringFromDOM(responseResult.getDocument()));

                    String result = responseResult.getString("/api/edit/@result");
                    if(!result.equals("Success")) {
                        // FIXME: Log this somewhere else
                        Log.d("Commons", "Non success result!" + result);
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
