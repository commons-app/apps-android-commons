package fr.free.nrw.commons.notification;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Optional;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

/**
 * Created by root on 18.12.2017.
 */

public class NotificationActivity extends NavigationBaseActivity {
    NotificationAdapterFactory notificationAdapterFactory;

    @Nullable
    @BindView(R.id.listView) RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        ButterKnife.bind(this);
        initListView();
        addNotifications();
        initDrawer();
    }

    private void initListView() {
        recyclerView = findViewById(R.id.listView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapterFactory = new NotificationAdapterFactory(new NotificationRenderer.NotificationClicked() {
            @Override
            public void notificationClicked(Notification notification) {

            }
        });
    }

    private void addNotifications() {

        recyclerView.setAdapter(notificationAdapterFactory.create(NotificationController.loadNotifications()));
    }
}
