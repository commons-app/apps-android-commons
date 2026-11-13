package fr.free.nrw.commons.media

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Helper class for handling media violation reports.
 * Responsible for presenting the violation report dialog, constructing technical
 * report details, and dispatching report emails.
 */
class MediaReportHelper
    @Inject
    constructor(
        private val sessionManager: SessionManager,
    ) {
        /**
         * Displays an alert dialog allowing the user to select a violation reason for reporting the media.
         *
         * @param activity the activity context used to show the dialog
         * @param media the media item being reported
         */
        fun showReportDialog(
            activity: Activity,
            media: Media?,
        ) {
            if (media == null) {
                return
            }
            val builder = AlertDialog.Builder(activity)
            val values =
                activity
                    .resources
                    .getStringArray(R.array.report_violation_options)
            builder.setTitle(R.string.report_violation)
            builder.setItems(
                R.array.report_violation_options,
            ) { dialog: DialogInterface?, which: Int ->
                sendReportEmail(activity, media, values[which])
            }
            builder.setNegativeButton(
                R.string.cancel,
            ) { dialog: DialogInterface?, which: Int -> }
            builder.setCancelable(false)
            builder.show()
        }

        /**
         * Constructs and dispatches an email intent to report a violation for the specified media.
         *
         * @param context the context used to launch the email intent and display feedback
         * @param media the media item being reported
         * @param type the selected violation type
         */
        fun sendReportEmail(
            context: Context,
            media: Media,
            type: String,
        ) {
            val technicalInfo = getTechInfo(media, type)

            val feedbackIntent = Intent(Intent.ACTION_SENDTO)
            feedbackIntent.setType("message/rfc822")
            feedbackIntent.setData(Uri.parse("mailto:"))
            feedbackIntent.putExtra(
                Intent.EXTRA_EMAIL,
                arrayOf(CommonsApplication.REPORT_EMAIL),
            )
            feedbackIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                CommonsApplication.REPORT_EMAIL_SUBJECT,
            )
            feedbackIntent.putExtra(Intent.EXTRA_TEXT, technicalInfo)
            try {
                context.startActivity(feedbackIntent)
            } catch (e: ActivityNotFoundException) {
                Timber.e(e, "No email client available to send feedback")
                Toast.makeText(context, R.string.no_email_client, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Formats technical and contextual information into a report string for the email body.
         *
         * @param media the media item being reported
         * @param type the selected violation type
         * @return the formatted report body text
         */
        fun getTechInfo(
            media: Media,
            type: String,
        ): String {
            val builder = StringBuilder()

            builder
                .append("Report type: ")
                .append(type)
                .append("\n\n")

            builder
                .append("Image that you want to report: ")
                .append(media.imageUrl)
                .append("\n\n")

            builder
                .append("User that you want to report: ")
                .append(media.user)
                .append("\n\n")

            if (sessionManager.userName != null) {
                builder
                    .append("Your username: ")
                    .append(sessionManager.userName)
                    .append("\n\n")
            }

            builder
                .append("Violation reason: ")
                .append("\n")

            builder
                .append("----------------------------------------------")
                .append("\n")
                .append("(please write reason here)")
                .append("\n")
                .append("----------------------------------------------")
                .append("\n\n")
                .append("Thank you for your report! Our team will investigate as soon as possible.")
                .append("\n")
                .append("Please note that images also have a `Nominate for deletion` button.")

            return builder.toString()
        }
    }
