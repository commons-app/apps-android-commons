package fr.free.nrw.commons.notification;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.kvstore.BasicKvStore;
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

    @BindView(R.id.listView)
    RecyclerView recyclerView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.container)
    RelativeLayout relativeLayout;
    @BindView(R.id.no_notification_background)
    ConstraintLayout no_notification;
   /* @BindView(R.id.swipe_bg)
    TextView swipe_bg;*/

    @Inject
    NotificationController controller;
    @Inject
    MediaWikiApi mediaWikiApi;
    @Inject
    @Named("last_read_notification_date")
    BasicKvStore kvStore;

    private static final String TAG_NOTIFICATION_WORKER_FRAGMENT = "NotificationWorkerFragment";
    private NotificationWorkerFragment mNotificationWorkerFragment;
    private RVRendererAdapter<Notification> adapter;
    private List<Notification> notificationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        ButterKnife.bind(this);
        mNotificationWorkerFragment = (NotificationWorkerFragment) getFragmentManager()
                .findFragmentByTag(TAG_NOTIFICATION_WORKER_FRAGMENT);
        initListView();
        initDrawer();
        setUpItemTouchHelper();
    }

    private void setUpItemTouchHelper() {

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            // we want to cache these and not allocate anything repeatedly in the onChildDraw method
            Drawable background;
            int xMarkMargin;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.RED);
                xMarkMargin=120;
                initiated = true;
            }

            // not important, we don't want drag & drop
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            /*@Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                return super.getSwipeDirs(recyclerView, viewHolder);
            }*/

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                Observable.fromCallable(() -> controller.markAsRead(notificationList.get(position)))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result){
                                notificationList.remove(position);
                                setAdapter(notificationList);
                                adapter.notifyDataSetChanged();
                                Snackbar snackbar = Snackbar
                                        .make(relativeLayout,"Notification marked as read", Snackbar.LENGTH_LONG);

                                snackbar.show();
                                if (notificationList.size()==0){
                                    relativeLayout.setVisibility(View.GONE);
                                    no_notification.setVisibility(View.VISIBLE);
                                }
                            }
                            else {
                                adapter.notifyDataSetChanged();
                                setAdapter(notificationList);
                                Toast.makeText(NotificationActivity.this, "There was some error!", Toast.LENGTH_SHORT).show();
                            }
                        }, throwable -> {
                            Timber.e(throwable, "Error occurred while loading notifications");
                            ViewUtil.showShortSnackbar(relativeLayout, R.string.error_notifications);
                            progressBar.setVisibility(View.GONE);
                        });

            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                if (viewHolder.getAdapterPosition() == -1) {
                    return;
                }
                if (!initiated) {
                    init();
                }

                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);
                super.onChildDraw(c, recyclerView, viewHolder, dX/4, dY, actionState, isCurrentlyActive);
            }

        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
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
            Snackbar.make(relativeLayout, R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry, view -> refresh()).show();
        } else {
            progressBar.setVisibility(View.VISIBLE);
            addNotifications();
        }
    }

    @SuppressLint("CheckResult")
    private void addNotifications() {
        Timber.d("Add notifications");

        // Store when add notification is called last
        long currentDate = new Date(System.currentTimeMillis()).getTime();
        kvStore.putLong("last_read_notification_date", currentDate);
        Timber.d("Set last notification read date to current date:" + currentDate);

        if (mNotificationWorkerFragment == null) {
            Observable.fromCallable(() -> {
                progressBar.setVisibility(View.VISIBLE);
                return controller.getNotifications();
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(notificationList -> {
                        Collections.reverse(notificationList);
                        Timber.d("Number of notifications is %d", notificationList.size());
                        this.notificationList = notificationList;
                        if (notificationList.size()==0){
                            relativeLayout.setVisibility(View.GONE);
                            no_notification.setVisibility(View.VISIBLE);
                        } else {
                            setAdapter(notificationList);
                        }
                        progressBar.setVisibility(View.GONE);
                    }, throwable -> {
                        Timber.e(throwable, "Error occurred while loading notifications");
                        ViewUtil.showShortSnackbar(relativeLayout, R.string.error_notifications);
                        progressBar.setVisibility(View.GONE);
                    });
        } else {
            notificationList = mNotificationWorkerFragment.getNotificationList();
            setAdapter(notificationList);
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
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            no_notification.setVisibility(View.VISIBLE);
            return;
        }
        notificationAdapterFactory = new NotificationAdapterFactory(notification -> {
            Timber.d("Notification clicked %s", notification.link);
            handleUrl(notification.link);
        });
        adapter = notificationAdapterFactory.create(notificationList);
        recyclerView.setAdapter(adapter);
    }

    public static void startYourself(Context context) {
        Intent intent = new Intent(context, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    private void initializeAndSetNotificationList(List<Notification> notificationList) {
        FragmentManager fm = getFragmentManager();
        mNotificationWorkerFragment = new NotificationWorkerFragment();
        fm.beginTransaction().add(mNotificationWorkerFragment, TAG_NOTIFICATION_WORKER_FRAGMENT)
                .commit();
        mNotificationWorkerFragment.setNotificationList(notificationList);
    }
}
