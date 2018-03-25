package fr.free.nrw.commons.notification;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
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
        addNotifications();
    }

    @SuppressLint("CheckResult")
    private void addNotifications() {
        Timber.d("Add notifications");

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
                        ViewUtil.showSnackbar(relativeLayout, R.string.error_notifications);
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
        if(notificationList == null || notificationList.isEmpty()) {
            ViewUtil.showSnackbar(relativeLayout, R.string.no_notifications);
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
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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