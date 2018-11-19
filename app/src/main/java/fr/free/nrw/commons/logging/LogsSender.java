package fr.free.nrw.commons.logging;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.apache.commons.codec.Charsets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import fr.free.nrw.commons.auth.SessionManager;
import timber.log.Timber;

/**
 * Abstract class that implements Acra's log sender
 */
public abstract class LogsSender implements ReportSender {

    String mailTo;
    String logFileName;
    String emailSubject;
    String emailBody;

    private final SessionManager sessionManager;
    private final boolean isBeta;

    LogsSender(SessionManager sessionManager,
               boolean isBeta) {
        this.sessionManager = sessionManager;
        this.isBeta = isBeta;
    }

    /**
     * Overrides send method of ACRA's ReportSender to send logs
     * @param context
     * @param report
     */
    @Override
    public void send(@NonNull final Context context, @Nullable CrashReportData report) {
        sendLogs(context, report);
    }

    /**
     * Gets zipped log files and sends it via email. Can be modified to change the send log mechanism
     * @param context
     * @param report
     */
    private void sendLogs(Context context, CrashReportData report) {
        final Uri logFileUri = getZippedLogFileUri(context, report);
        if (logFileUri != null) {
            sendEmail(context, logFileUri);
        }
    }

    /***
     * Provides any extra information that you want to send. The return value will be
     * delivered inside the report verbatim
     *
     * @return
     */
    protected abstract String getExtraInfo();

    /**
     * Fires an intent to send email with logs
     * @param context
     * @param logFileUri
     */
    private void sendEmail(Context context, Uri logFileUri) {
        String subject = emailSubject;
        String body = emailBody;

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.fromParts("mailto", mailTo, null));
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putExtra(Intent.EXTRA_STREAM, logFileUri);
        context.startActivity(emailIntent);
    }

    /**
     * Returns the URI for the zipped log file
     * @param context
     * @param report
     * @return
     */
    private Uri getZippedLogFileUri(Context context, CrashReportData report) {
        try {
            StringBuilder builder = new StringBuilder();
            if (report != null) {
                attachCrashInfo(report, builder);
            }
            attachUserInfo(builder);
            attachExtraInfo(builder);
            byte[] metaData = builder.toString().getBytes(Charsets.UTF_8);
            File zipFile = new File(context.getExternalFilesDir(null), logFileName);
            writeLogToZipFile(metaData, zipFile);
            return Uri.fromFile(zipFile);
        } catch (IOException e) {
            Timber.w(e, "Error in generating log file");
        }
        return null;
    }

    /**
     * Checks if there are any pending crash reports and attaches them to the logs
     * @param report
     * @param builder
     */
    private void attachCrashInfo(CrashReportData report, StringBuilder builder) {
        if (report == null) {
            return;
        }
        builder.append(report);
    }

    /**
     * Attaches username to the the meta_data file
     * @param builder
     */
    private void attachUserInfo(StringBuilder builder) {
        builder.append("MediaWiki Username = ").append(sessionManager.getUserName()).append("\n");
    }

    /**
     * Gets any extra meta information to be attached with the log files
     * @param builder
     */
    private void attachExtraInfo(StringBuilder builder) {
        String infoToBeAttached = getExtraInfo();
        builder.append(infoToBeAttached);
        builder.append("\n");
    }

    /**
     * Zips the logs and meta information
     * @param metaData
     * @param zipFile
     * @throws IOException
     */
    private void writeLogToZipFile(byte[] metaData, File zipFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ZipOutputStream zos = new ZipOutputStream(bos);
        File logDir = new File(LogUtils.getLogDirectory(isBeta));

        if (!logDir.exists() || logDir.listFiles().length == 0) {
            return;
        }
        
        byte[] buffer = new byte[1024];
        for (File file : logDir.listFiles()) {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            zos.putNextEntry(new ZipEntry(file.getName()));
            int length;
            while ((length = bis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
            bis.close();
        }

        //attach metadata as a separate file
        zos.putNextEntry(new ZipEntry("meta_data.txt"));
        zos.write(metaData);
        zos.closeEntry();

        zos.flush();
        zos.close();
    }
}
