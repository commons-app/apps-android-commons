package fr.free.nrw.commons.notification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

/**
 * Created by root on 19.12.2017.
 */
@Singleton
public class NotificationController {

    private MediaWikiApi mediaWikiApi;
    private SessionManager sessionManager;

    @Inject
    public NotificationController(MediaWikiApi mediaWikiApi, SessionManager sessionManager) {
        this.mediaWikiApi = mediaWikiApi;
        this.sessionManager = sessionManager;
    }

    public List<Notification> getNotifications() throws IOException {
        if (mediaWikiApi.validateLogin()) {
            return mediaWikiApi.getNotifications();
        } else {
            Boolean authTokenValidated = sessionManager.revalidateAuthToken();
            if (authTokenValidated != null && authTokenValidated) {
                return mediaWikiApi.getNotifications();
            }
        }
        return new ArrayList<>();
    }
}
