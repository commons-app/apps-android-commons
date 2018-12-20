package fr.free.nrw.commons.logging;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.auth.SessionManager;

/**
 * Class responsible for sending logs to developers
 */
@Singleton
public class CommonsLogSender extends LogsSender {
    private static final String LOGS_PRIVATE_EMAIL = "commons-app-android-private@googlegroups.com";
    private static final String LOGS_PRIVATE_EMAIL_SUBJECT = "Commons Android App (%s) Logs";
    private static final String BETA_LOGS_PRIVATE_EMAIL_SUBJECT = "Commons Beta Android App (%s) Logs";

    @Inject
    public CommonsLogSender(SessionManager sessionManager,
                            @Named("isBeta") boolean isBeta) {
        super(sessionManager, isBeta);

        this.logFileName = isBeta ? "CommonsBetaAppLogs.zip" : "CommonsAppLogs.zip";
        String emailSubjectFormat = isBeta ? BETA_LOGS_PRIVATE_EMAIL_SUBJECT : LOGS_PRIVATE_EMAIL_SUBJECT;
        String message = String.format(emailSubjectFormat, sessionManager.getUserName());
        this.emailSubject = message;
        this.emailBody = message;
        this.mailTo = LOGS_PRIVATE_EMAIL;
    }

    /**
     * Attach any extra meta information about user or device that might help in debugging
     * @return
     */
    @Override
    protected String getExtraInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("App Version Name: ")
                .append(BuildConfig.VERSION_NAME)
                .append("\n");

        return builder.toString();
    }
}
