package fr.free.nrw.commons.logging

import android.content.Context

import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton

import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.utils.ConfigUtils
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import fr.free.nrw.commons.utils.DeviceInfoUtil
import org.acra.data.CrashReportData


/**
 * Class responsible for sending logs to developers
 */
@Singleton
class CommonsLogSender @Inject constructor(
    private val sessionManager: SessionManager,
    private val context: Context
) : LogsSender(sessionManager) {


    companion object {
        private const val LOGS_PRIVATE_EMAIL = "commons-app-android-private@googlegroups.com"
        private const val LOGS_PRIVATE_EMAIL_SUBJECT = "Commons Android App (%s) Logs"
        private const val BETA_LOGS_PRIVATE_EMAIL_SUBJECT = "Commons Beta Android App (%s) Logs"
    }

    init {
        val isBeta = ConfigUtils.isBetaFlavour
        logFileName = if (isBeta) "CommonsBetaAppLogs.zip" else "CommonsAppLogs.zip"
        val emailSubjectFormat = if (isBeta)
            BETA_LOGS_PRIVATE_EMAIL_SUBJECT
        else
            LOGS_PRIVATE_EMAIL_SUBJECT
        emailSubject = emailSubjectFormat.format(sessionManager.userName)
        emailBody = getExtraInfo()
        mailTo = LOGS_PRIVATE_EMAIL
    }

    /**
     * Attach any extra meta information about the user or device that might help in debugging.
     * @return String with extra meta information useful for debugging.
     */
    public override fun getExtraInfo(): String {
        return buildString {
            // Getting API Level
            append("API level: ")
                .append(DeviceInfoUtil.getAPILevel())
                .append("\n")

            // Getting Android Version
            append("Android version: ")
                .append(DeviceInfoUtil.getAndroidVersion())
                .append("\n")

            // Getting Device Manufacturer
            append("Device manufacturer: ")
                .append(DeviceInfoUtil.getDeviceManufacturer())
                .append("\n")

            // Getting Device Model
            append("Device model: ")
                .append(DeviceInfoUtil.getDeviceModel())
                .append("\n")

            // Getting Device Name
            append("Device: ")
                .append(DeviceInfoUtil.getDevice())
                .append("\n")

            // Getting Network Type
            append("Network type: ")
                .append(DeviceInfoUtil.getConnectionType(context))
                .append("\n")

            // Getting App Version
            append("App version name: ")
                .append(context.getVersionNameWithSha())
                .append("\n")

            // Getting Username
            append("User name: ")
                .append(sessionManager.userName)
                .append("\n")
        }
    }

    /**
     * Determines if the log sending process requires the app to be in the foreground.
     * @return False as it does not require foreground execution.
     */
    override fun requiresForeground(): Boolean = false

    /**
     * Sends logs to developers. Implementation can be extended.
     */
    override fun send(
        context: Context,
        errorContent: CrashReportData,
        extras: Bundle) {
        // Add logic here if needed.
    }
}