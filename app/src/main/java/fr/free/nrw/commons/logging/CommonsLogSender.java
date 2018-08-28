package fr.free.nrw.commons.logging;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.auth.SessionManager;

import static fr.free.nrw.commons.CommonsApplication.FEEDBACK_EMAIL_SUBJECT;

@Singleton
public class CommonsLogSender extends LogsSender {

    @Inject
    public CommonsLogSender(SessionManager sessionManager) {
        super(sessionManager);
        this.logFileName = "CommonsAppLogs.zip";
        String message = String.format(FEEDBACK_EMAIL_SUBJECT, sessionManager.getUserName());
        this.emailSubject = message;
        this.emailBody = message;
    }

    @Override
    protected String getExtraInfo() {
        return "No extra information to attach";
    }
}