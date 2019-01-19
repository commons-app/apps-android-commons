package fr.free.nrw.commons.logging;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.DeviceInfoUtil;

/**
 * Class responsible for sending logs to developers
 */
@Singleton
public class CommonsLogSender extends LogsSender {
    private static final String LOGS_PRIVATE_EMAIL = "commons-app-android-private@googlegroups.com";
    private static final String LOGS_PRIVATE_EMAIL_SUBJECT = "Commons Android App (%s) Logs";
    private static final String BETA_LOGS_PRIVATE_EMAIL_SUBJECT = "Commons Beta Android App (%s) Logs";

    private SessionManager sessionManager;
    private Context context;

    @Inject
    public CommonsLogSender(SessionManager sessionManager,
                            Context context) {
        super(sessionManager);

        this.sessionManager = sessionManager;
        this.context = context;
        boolean isBeta = ConfigUtils.isBetaFlavour();
        this.logFileName = isBeta ? "CommonsBetaAppLogs.zip" : "CommonsAppLogs.zip";
        String emailSubjectFormat = isBeta ? BETA_LOGS_PRIVATE_EMAIL_SUBJECT : LOGS_PRIVATE_EMAIL_SUBJECT;
        this.emailSubject = String.format(emailSubjectFormat, sessionManager.getUserName());
        this.emailBody = getExtraInfo();
        this.mailTo = LOGS_PRIVATE_EMAIL;
    }

    /**
     * Attach any extra meta information about user or device that might help in debugging
     * @return String with extra meta information useful for debugging
     */
    @Override
    protected String getExtraInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("App Version Name: ")
                .append(BuildConfig.VERSION_NAME)
                .append("\n");

        builder.append("User Name: ")
                .append(sessionManager.getUserName())
                .append("\n");

        builder.append("Network Type: ")
                .append(DeviceInfoUtil.getConnectionType(context))
                .append("\n");

        builder.append("Device manufacturer: ")
                .append(DeviceInfoUtil.getDeviceManufacturer())
                .append("\n");

        builder.append("Device model: ")
                .append(DeviceInfoUtil.getDeviceModel())
                .append("\n");

        builder.append("Android Version: ")
                .append(DeviceInfoUtil.getAndroidVersion())
                .append("\n");

        return builder.toString();
    }
}
