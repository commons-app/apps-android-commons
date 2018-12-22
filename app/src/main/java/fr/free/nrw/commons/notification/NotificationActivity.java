package fr.free.nrw.commons.notification;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by root on 18.12.2017.
 */

public class NotificationActivity extends NavigationBaseActivity {
    NotificationAdapterFactory notificationAdapterFactory;

    @BindView(R.id.listView) RecyclerView recyclerView;
    @BindView(R.id.progressBar) ProgressBar progressBar;
    @BindView(R.id.container) RelativeLayout relativeLayout;

    @Inject NotificationController controller;
    @Inject
    MediaWikiApi mediaWikiApi;

    private static final String TAG_NOTIFICATION_WORKER_FRAGMENT = "NotificationWorkerFragment";
    private NotificationWorkerFragment mNotificationWorkerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        ButterKnife.bind(this);
        mNotificationWorkerFragment = (NotificationWorkerFragment) getFragmentManager()
                                      .findFragmentByTag(TAG_NOTIFICATION_WORKER_FRAGMENT);
        initListView();
        initDrawer();
    }

    private void initListView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecor = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecor);
        refresh();
    }

    private void refresh() {
        if (!NetworkUtils.isInternetConnectionEstablished(this)) {
            progressBar.setVisibility(View.GONE);
            Snackbar.make(relativeLayout , R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry, view -> refresh()).show();
        }else {
            progressBar.setVisibility(View.VISIBLE);
            addNotifications();
        }
    }

    @SuppressLint("CheckResult")
    private void addNotifications() {
        Timber.d("Add notifications");

        // Store when add notification is called last
        long currentDate = new Date(System.currentTimeMillis()).getTime();
        getSharedPreferences("prefs", MODE_PRIVATE).edit().putLong("last_read_notification_date", currentDate).apply();
        Timber.d("Set last notification read date to current date:"+ currentDate);

        if(mNotificationWorkerFragment == null){
            Observable.fromCallable(() -> {
                progressBar.setVisibility(View.VISIBLE);
                return controller.getNotifications();
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(notificationList -> {
                        Collections.reverse(notificationList);
                        Timber.d("Number of notifications is %d", notificationList.size());
                        setAdapter(notificationList);
                        progressBar.setVisibility(View.GONE);
                    }, throwable -> {
                        Timber.e(throwable, "Error occurred while loading notifications");
                        ViewUtil.showShortSnackbar(relativeLayout, R.string.error_notifications);
                        progressBar.setVisibility(View.GONE);
                    });
        } else {
            setAdapter(mNotificationWorkerFragment.getNotificationList());
        }
    }

    private void handleUrl(String url) {
        if (url == null || url.equals("")) {
            return;
        }
        Utils.handleWebUrl(this, Uri.parse(url));
    }

    private void setAdapter(List<Notification> notificationList) {
        if (notificationList == null || notificationList.isEmpty()) {
            ViewUtil.showShortSnackbar(relativeLayout, R.string.no_notifications);
            return;
        }
        notificationAdapterFactory = new NotificationAdapterFactory(notification -> {
            Timber.d("Notification clicked %s", notification.link);
            handleUrl(notification.link);
        });
        RVRendererAdapter<Notification> adapter = notificationAdapterFactory.create(notificationList);
        recyclerView.setAdapter(adapter);
    }

    public static void startYourself(Context context) {
        Intent intent = new Intent(context, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    private void initializeAndSetNotificationList(List<Notification> notificationList){
        FragmentManager fm = getFragmentManager();
        mNotificationWorkerFragment = new NotificationWorkerFragment();
        fm.beginTransaction().add(mNotificationWorkerFragment, TAG_NOTIFICATION_WORKER_FRAGMENT)
                .commit();
        mNotificationWorkerFragment.setNotificationList(notificationList);
    }
}
