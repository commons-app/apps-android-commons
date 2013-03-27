package org.wikimedia.commons.modifications;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.*;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.accounts.Account;
import android.os.Bundle;

import java.io.*;
import java.util.*;

import org.mediawiki.api.*;
import org.wikimedia.commons.Utils;
import org.wikimedia.commons.*;
import org.wikimedia.commons.contributions.Contribution;
import org.wikimedia.commons.contributions.ContributionsContentProvider;


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

        CommonsApplication app = (CommonsApplication)getContext().getApplicationContext();
        String authCookie;
        try {
             authCookie = AccountManager.get(app).blockingGetAuthToken(account, "", false);
        } catch (OperationCanceledException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (AuthenticatorException e) {
            throw new RuntimeException(e);
        }

        MWApi api = app.getApi();
        api.setAuthCookie(authCookie);
        String editToken;

        ApiResult requestResult, responseResult;
        try {
            editToken = api.getEditToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                        throw new RuntimeException(e);
                    }

                    Log.d("Commons", "Page content is " + Utils.getStringFromDOM(requestResult.getDocument()));
                    String pageContent = requestResult.getString("/api/query/pages/page/revisions/rev");
                    String processedPageContent = sequence.executeModifications(contrib.getFilename(),  pageContent);

                    try {
                        responseResult = api.action("edit")
                                .param("title", contrib.getFilename())
                                .param("token", editToken)
                                .param("text", processedPageContent)
                                .post();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    Log.d("Commons", "Response is" + Utils.getStringFromDOM(responseResult.getDocument()));

                    String result = responseResult.getString("/api/edit/@result");
                    if(!result.equals("Success")) {
                        throw new RuntimeException();
                    }

                    sequence.delete();
                    allModifications.moveToNext();
                }

            }
        } finally {
            if(contributionsClient != null) {
                contributionsClient.release();
            }

        }
    }
}
