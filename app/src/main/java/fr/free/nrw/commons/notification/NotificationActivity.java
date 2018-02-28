package fr.free.nrw.commons.notification;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * Created by root on 18.12.2017.
 */

public class NotificationActivity extends NavigationBaseActivity {
    NotificationAdapterFactory notificationAdapterFactory;

    @BindView(R.id.listView) RecyclerView recyclerView;

    @Inject NotificationController controller;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        ButterKnife.bind(this);
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

        Observable.fromCallable(() -> controller.getNotifications())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(notificationList -> {
                    Collections.reverse(notificationList);
                    Timber.d("Number of notifications is %d", notificationList.size());
                    setAdapter(notificationList);
                }, throwable -> Timber.e(throwable, "Error occurred while loading notifications"));
    }

    private void handleUrl(String url) {
        if (url == null || url.equals("")) {
            return;
        }
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        //check if web browser available
        if(browser.resolveActivity(this.getPackageManager()) != null){
            startActivity(browser);
        } else {
            Toast toast = Toast.makeText(this, getString(R.string.no_web_browser), LENGTH_SHORT);
            toast.show();
        }
    }

    private void setAdapter(List<Notification> notificationList) {
        notificationAdapterFactory = new NotificationAdapterFactory(notification -> {
            Timber.d("Notification clicked %s", notification.link);
            handleUrl(notification.link);
        });
        RVRendererAdapter<Notification> adapter = notificationAdapterFactory.create(notificationList);
        recyclerView.setAdapter(adapter);
    }

    public static void startYourself(Context context) {
        Intent intent = new Intent(context, NotificationActivity.class);
        context.startActivity(intent);
    }
}
