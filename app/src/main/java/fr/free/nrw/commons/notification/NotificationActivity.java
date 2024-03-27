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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.databinding.ActivityNotificationBinding;
import fr.free.nrw.commons.notification.models.Notification;
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
    private ActivityNotificationBinding binding;

    @Inject
    NotificationController controller;

    private static final String TAG_NOTIFICATION_WORKER_FRAGMENT = "NotificationWorkerFragment";
    private NotificationWorkerFragment mNotificationWorkerFragment;
    private NotificatinAdapter adapter;
    private List<Notification> notificationList;
    MenuItem notificationMenuItem;
    /**
     * Boolean isRead is true if this notification activity is for read section of notification.
     */
    private boolean isRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isRead = getIntent().getStringExtra("title").equals("read");
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mNotificationWorkerFragment = (NotificationWorkerFragment) getFragmentManager()
                .findFragmentByTag(TAG_NOTIFICATION_WORKER_FRAGMENT);
        initListView();
        setPageTitle();
        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * If this is unread section of the notifications, removeNotification method
     *  Marks the notification as read,
     *  Removes the notification from unread,
     *  Displays the Snackbar.
     *
     * Otherwise returns (read section).
     *
     * @param notification
     */
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
                        ViewUtil.showLongSnackbar(binding.container,getString(R.string.notification_mark_read));
                        if (notificationList.size() == 0) {
                            setEmptyView();
                            binding.container.setVisibility(View.GONE);
                            binding.noNotificationBackground.setVisibility(View.VISIBLE);
                        }
                    } else {
                        adapter.notifyDataSetChanged();
                        setItems(notificationList);
                        ViewUtil.showLongToast(this,getString(R.string.some_error));
                    }
                }, throwable -> {

                    Timber.e(throwable, "Error occurred while loading notifications");
                    throwable.printStackTrace();
                    ViewUtil.showShortSnackbar(binding.container, R.string.error_notifications);
                    binding.progressBar.setVisibility(View.GONE);
                });
        compositeDisposable.add(disposable);
    }



    private void initListView() {
        binding.listView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecor = new DividerItemDecoration(binding.listView.getContext(), DividerItemDecoration.VERTICAL);
        binding.listView.addItemDecoration(itemDecor);
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
        binding.listView.setAdapter(adapter);
    }

    private void refresh(boolean archived) {
        if (!NetworkUtils.isInternetConnectionEstablished(this)) {
            binding.progressBar.setVisibility(View.GONE);
            Snackbar.make(binding.container, R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry, view -> refresh(archived)).show();
        } else {
            addNotifications(archived);
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.noNotificationBackground.setVisibility(View.GONE);
        binding.container.setVisibility(View.VISIBLE);
    }

    @SuppressLint("CheckResult")
    private void addNotifications(boolean archived) {
        Timber.d("Add notifications");
        if (mNotificationWorkerFragment == null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            compositeDisposable.add(controller.getNotifications(archived)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(notificationList -> {
                        Collections.reverse(notificationList);
                        Timber.d("Number of notifications is %d", notificationList.size());
                        this.notificationList = notificationList;
                        if (notificationList.size()==0){
                            setEmptyView();
                            binding.container.setVisibility(View.GONE);
                            binding.noNotificationBackground.setVisibility(View.VISIBLE);
                        } else {
                            setItems(notificationList);
                        }
                        binding.progressBar.setVisibility(View.GONE);
                    }, throwable -> {
                        Timber.e(throwable, "Error occurred while loading notifications");
                        ViewUtil.showShortSnackbar(binding.container, R.string.error_notifications);
                        binding.progressBar.setVisibility(View.GONE);
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
            ViewUtil.showShortSnackbar(binding.container, R.string.no_notifications);
            /*progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);*/
            binding.container.setVisibility(View.GONE);
            setEmptyView();
            binding.noNotificationBackground.setVisibility(View.VISIBLE);
            return;
        }
        binding.container.setVisibility(View.VISIBLE);
        binding.noNotificationBackground.setVisibility(View.GONE);
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
            binding.noNotificationText.setText(R.string.no_read_notification);
        }else {
            binding.noNotificationText.setText(R.string.no_notification);
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
