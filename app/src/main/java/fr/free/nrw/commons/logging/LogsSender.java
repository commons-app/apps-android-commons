package fr.free.nrw.commons.logging;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.acra.ACRA;
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

import static fr.free.nrw.commons.CommonsApplication.FEEDBACK_EMAIL;

public abstract class LogsSender implements ReportSender {

    String logFileName;
    String emailSubject;
    String emailBody;

    private final SessionManager sessionManager;

    LogsSender(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void send(@NonNull final Context context, @Nullable CrashReportData report) {
        Timber.d("Trying to send a new report %s", ACRA.isACRASenderServiceProcess());
        sendLogs(context, report);
    }

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

    private void sendEmail(Context context, Uri logFileUri) {
        String subject = emailSubject;
        String body = emailBody;

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.fromParts("mailto", FEEDBACK_EMAIL, null));
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putExtra(Intent.EXTRA_STREAM, logFileUri);
        context.startActivity(emailIntent);
    }

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
            writeLogToZipFile(context, metaData, zipFile);
            return Uri.fromFile(zipFile);
        } catch (IOException e) {
            Timber.w(e, "Error in generating log file");
        }
        return null;
    }

    private void attachCrashInfo(CrashReportData report, StringBuilder builder) {
        if (report == null) {
            return;
        }
        builder.append(report);
    }

    private void attachUserInfo(StringBuilder builder) {
        builder.append("MediaWiki Username = ").append(sessionManager.getUserName()).append("\n");
    }

    private void attachExtraInfo(StringBuilder builder) {
        String infoToBeAttached = getExtraInfo();
        builder.append(infoToBeAttached);
        builder.append("\n");
    }

    private void writeLogToZipFile(Context context, byte[] metaData, File zipFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ZipOutputStream zos = new ZipOutputStream(bos);
        File logDir = new File(LogUtils.getLogDirectory(context));
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