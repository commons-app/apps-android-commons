package fr.free.nrw.commons.auth;

import android.os.Bundle;

import javax.inject.Inject;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.UserClient;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public abstract class AuthenticatedActivity extends NavigationBaseActivity {

    @Inject
    protected SessionManager sessionManager;
    @Inject
    UserClient userClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showBlockStatus();
    }

    /**
     * Makes API call to check if user is blocked from Commons. If the user is blocked, a snackbar
     * is created to notify the user
     */
    protected void showBlockStatus() {
        compositeDisposable.add(userClient.isUserBlockedFromCommons()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(result -> result)
                .subscribe(result -> ViewUtil.showShortSnackbar(findViewById(android.R.id.content), R.string.block_notification)
                ));
    }
}
