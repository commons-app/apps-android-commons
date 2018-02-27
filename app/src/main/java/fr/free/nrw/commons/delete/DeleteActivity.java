package fr.free.nrw.commons.delete;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;

import javax.inject.Inject;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

public class DeleteActivity extends AppCompatActivity {

    @Inject MediaWikiApi mwApi;
    @Inject SessionManager sessionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ApplicationlessInjection
                .getInstance(this.getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

        setContentView(R.layout.activity_delete);

        String authCookie = sessionManager.getAuthCookie();
        Timber.d(authCookie);

        mwApi.setAuthCookie(authCookie);
        String editToken = "noooooooo";

        try {
            editToken = mwApi.getEditToken();
        } catch (Exception e) {
            Timber.d(e.getMessage());
        }
        Timber.d(editToken);

    }
}