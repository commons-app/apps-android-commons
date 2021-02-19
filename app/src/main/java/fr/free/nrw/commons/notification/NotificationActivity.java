package fr.free.nrw.commons.notification;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.snackbar.Snackbar;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import kotlin.Unit;
import timber.log.Timber;

/**
 * Created by root on 18.12.2017.
 */

public class NotificationActivity extends BaseActivity {
    @BindView(R.id.listView)
    RecyclerView recyclerView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.container)
    RelativeLayout relativeLayout;
    @BindView(R.id.no_notification_background)
    ConstraintLayout no_notification;
    @BindView(R.id.no_notification_text)
    TextView noNotificationText;
    @BindView(R.id.toolbar)
    Toolbar toolbar;


    @Inject
    NotificationController controller;

    private static final String TAG_NOTIFICATION_WORKER_FRAGMENT = "NotificationWorkerFragment";
    private NotificationWorkerFragment mNotificationWorkerFragment;
    private NotificatinAdapter adapter;
    private List<Notification> notificationList;
    private boolean isRead;
    MenuItem notificationMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isRead = getIntent().getStringExtra("title").equals("read");
        setContentView(R.layout.activity_notification);
        ButterKnife.bind(this);
        mNotificationWorkerFragment = (NotificationWorkerFragment) getFragmentManager()
                .findFragmentByTag(TAG_NOTIFICATION_WORKER_FRAGMENT);
        initListView();
        setPageTitle();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @SuppressLint("CheckResult")
    public void removeNotification(Notification notification) {
        if (isRead) {
            return;
        }
        Disposable disposable = Observable.defer((Callable<ObservableSource<Boolean>>)
                () -> controller.markAsRead(notification))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result) {
                        notificationList.remove(notification);
                        setItems(notificationList);
                        adapter.notifyDataSetChanged();
                        Snackbar snackbar = Snackbar
                                .make(relativeLayout, getString(R.string.notification_mark_read), Snackbar.LENGTH_LONG);

                        snackbar.show();
                        if (notificationList.size() == 0) {
                            setEmptyView();
                            relativeLayout.setVisibility(View.GONE);
                            no_notification.setVisibility(View.VISIBLE);
                        }
                    } else {
                        adapter.notifyDataSetChanged();
                        setItems(notificationList);
                        Toast.makeText(NotificationActivity.this, getString(R.string.some_error), Toast.LENGTH_SHORT).show();
                    }
                }, throwable -> {

                    Timber.e(throwable, "Error occurred while loading notifications");
                    throwable.printStackTrace();
                    ViewUtil.showShortSnackbar(relativeLayout, R.string.error_notifications);
                    progressBar.setVisibility(View.GONE);
                });
        compositeDisposable.add(disposable);
    }



    private void initListView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecor = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecor);
        if (isRead) {
            refresh(true);
        } else {
            refresh(false);
        }
        adapter = new NotificatinAdapter(item -> {
            Timber.d("Notification clicked %s", item.getLink());
            handleUrl(item.getLink());
            removeNotification(item);
            return Unit.INSTANCE;
        });
        recyclerView.setAdapter(this.adapter);
    }

    private void refresh(boolean archived) {
        if (!NetworkUtils.isInternetConnectionEstablished(this)) {
            progressBar.setVisibility(View.GONE);
            Snackbar.make(relativeLayout, R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry, view -> refresh(archived)).show();
        } else {
            addNotifications(archived);
        }
        progressBar.setVisibility(View.VISIBLE);
        no_notification.setVisibility(View.GONE);
        relativeLayout.setVisibility(View.VISIBLE);
    }

    @SuppressLint("CheckResult")
    private void addNotifications(boolean archived) {
        Timber.d("Add notifications");
        if (mNotificationWorkerFragment == null) {
            progressBar.setVisibility(View.VISIBLE);
            compositeDisposable.add(controller.getNotifications(archived)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(notificationList -> {
                        Collections.reverse(notificationList);
                        Timber.d("Number of notifications is %d", notificationList.size());
                        this.notificationList = notificationList;
                        if (notificationList.size()==0){
                            setEmptyView();
                            relativeLayout.setVisibility(View.GONE);
                            no_notification.setVisibility(View.VISIBLE);
                        } else {
                            setItems(notificationList);
                        }
                        progressBar.setVisibility(View.GONE);
                    }, throwable -> {
                        Timber.e(throwable, "Error occurred while loading notifications");
                        ViewUtil.showShortSnackbar(relativeLayout, R.string.error_notifications);
                        progressBar.setVisibility(View.GONE);
                    }));
        } else {
            notificationList = mNotificationWorkerFragment.getNotificationList();
            setItems(notificationList);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_notifications, menu);
        notificationMenuItem = menu.findItem(R.id.archived);
        setMenuItemTitle();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.archived:
                if (item.getTitle().equals(getString(R.string.menu_option_read))) {
                    NotificationActivity.startYourself(NotificationActivity.this, "read");
                }else if (item.getTitle().equals(getString(R.string.menu_option_unread))) {
                    onBackPressed();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleUrl(String url) {
        if (url == null || url.equals("")) {
            return;
        }
        Utils.handleWebUrl(this, Uri.parse(url));
    }

    private void setItems(List<Notification> notificationList) {
        if (notificationList == null || notificationList.isEmpty()) {
            ViewUtil.showShortSnackbar(relativeLayout, R.string.no_notifications);
            /*progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);*/
            relativeLayout.setVisibility(View.GONE);
            setEmptyView();
            no_notification.setVisibility(View.VISIBLE);
            return;
        }
        relativeLayout.setVisibility(View.VISIBLE);
        no_notification.setVisibility(View.GONE);
        adapter.setItems(notificationList);
    }

    public static void startYourself(Context context, String title) {
        Intent intent = new Intent(context, NotificationActivity.class);
        intent.putExtra("title", title);

        context.startActivity(intent);
    }

    private void setPageTitle() {
        if (getSupportActionBar() != null) {
            if (isRead) {
                getSupportActionBar().setTitle(R.string.read_notifications);
            } else {
                getSupportActionBar().setTitle(R.string.notifications);
            }
        }
    }

    private void setEmptyView() {
        if (isRead) {
            noNotificationText.setText(R.string.no_read_notification);
        }else {
            noNotificationText.setText(R.string.no_notification);
        }
    }

    private void setMenuItemTitle() {
        if (isRead) {
            notificationMenuItem.setTitle(R.string.menu_option_unread);

        }else {
            notificationMenuItem.setTitle(R.string.menu_option_read);

        }
    }
}
