package fr.free.nrw.commons.logging

import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.core.content.FileProvider

import org.acra.data.CrashReportData
import org.acra.sender.ReportSender

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import org.acra.ACRA.errorReporter
import timber.log.Timber


/**
 * Abstract class that implements Acra's log sender.
 */
abstract class LogsSender(
    private val sessionManager: SessionManager
): ReportSender {

    var mailTo: String? = null
    var logFileName: String? = null
    var emailSubject: String? = null
    var emailBody: String? = null

    /**
     * Overrides the send method of ACRA's ReportSender to send logs.
     *
     * @param context The context in which to send the logs.
     * @param report The crash report data, if any.
     */
    fun sendWithNullable(context: Context, report: CrashReportData?) {
        if (report == null) {
            errorReporter.handleSilentException(null)
            return
        }
        send(context, report)
    }

    override fun send(context: Context, report: CrashReportData) {
        sendLogs(context, report)
    }

    /**
     * Gets zipped log files and sends them via email. Can be modified to change the send
     * log mechanism.
     *
     * @param context The context in which to send the logs.
     * @param report The crash report data, if any.
     */
    private fun sendLogs(context: Context, report: CrashReportData?) {
        val logFileUri = getZippedLogFileUri(context, report)
        if (logFileUri != null) {
            sendEmail(context, logFileUri)
        } else {
            errorReporter.handleSilentException(null)

        }
    }

    /**
     * Provides any extra information that you want to send. The return value will be
     * delivered inside the report verbatim.
     *
     * @return A string containing the extra information.
     */
    protected abstract fun getExtraInfo(): String

    /**
     * Fires an intent to send an email with logs.
     *
     * @param context The context in which to send the email.
     * @param logFileUri The URI of the zipped log file.
     */
    private fun sendEmail(context: Context, logFileUri: Uri) {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(mailTo))
            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
            putExtra(Intent.EXTRA_STREAM, logFileUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.share_logs_using)))
    }

    /**
     * Returns the URI for the zipped log file.
     *
     * @param context The context for file URI generation.
     * @param report The crash report data, if any.
     * @return The URI of the zipped log file or null if an error occurs.
     */
    private fun getZippedLogFileUri(context: Context, report: CrashReportData?): Uri? {
        return try {
            val builder = StringBuilder().apply {
                report?.let { attachCrashInfo(it, this) }
                attachUserInfo(this)
                attachExtraInfo(this)
            }
            val metaData = builder.toString().toByteArray(Charsets.UTF_8)
            val zipFile = File(LogUtils.getLogZipDirectory(), logFileName ?: "logs.zip")
            writeLogToZipFile(metaData, zipFile)
            FileProvider.getUriForFile(
                context,
                "${context.applicationContext.packageName}.provider",
                zipFile
            )
        } catch (e: IOException) {
            Timber.w(e, "Error in generating log file")
            null
        }
    }

    /**
     * Checks if there are any pending crash reports and attaches them to the logs.
     *
     * @param report The crash report data, if any.
     * @param builder The string builder to append crash info.
     */
    private fun attachCrashInfo(report: CrashReportData?, builder: StringBuilder) {
        if(report != null) {
            builder.append(report)
        }
    }

    /**
     * Attaches the username to the metadata file.
     *
     * @param builder The string builder to append user info.
     */
    private fun attachUserInfo(builder: StringBuilder) {
        builder.append("MediaWiki Username = ").append(sessionManager.userName).append("\n")
    }

    /**
     * Gets any extra metadata information to be attached with the log files.
     *
     * @param builder The string builder to append extra info.
     */
    private fun attachExtraInfo(builder: StringBuilder) {
        builder.append(getExtraInfo()).append("\n")
    }

    /**
     * Zips the logs and metadata information.
     *
     * @param metaData The metadata to be added to the zip file.
     * @param zipFile The zip file to write to.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun writeLogToZipFile(metaData: ByteArray, zipFile: File) {
        val logDir = File(LogUtils.getLogDirectory())
        if (!logDir.exists() || logDir.listFiles().isNullOrEmpty()) return

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            val buffer = ByteArray(1024)
            logDir.listFiles()?.forEach { file ->
                if (file.isDirectory) return@forEach
                FileInputStream(file).use { fis ->
                    BufferedInputStream(fis).use { bis ->
                        zos.putNextEntry(ZipEntry(file.name))
                        var length: Int
                        while (bis.read(buffer).also { length = it } > 0) {
                            zos.write(buffer, 0, length)
                        }
                        zos.closeEntry()
                    }
                }
            }

            // Attach metadata as a separate file.
            zos.putNextEntry(ZipEntry("meta_data.txt"))
            zos.write(metaData)
            zos.closeEntry()
        }
    }
}
