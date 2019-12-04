package fr.free.nrw.commons.notification;

import android.app.Fragment;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Created by knightshade on 25/2/18.
 */

public class NotificationWorkerFragment extends Fragment {
    private List<Notification> notificationList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setNotificationList(List<Notification> notificationList){
        this.notificationList = notificationList;
    }

    public List<Notification> getNotificationList(){
        return notificationList;
    }
}
