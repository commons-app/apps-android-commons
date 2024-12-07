package fr.free.nrw.commons.feedback

import android.content.Context
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.getUserName
import fr.free.nrw.commons.feedback.model.Feedback
import fr.free.nrw.commons.utils.LangCodeUtils.getLocalizedResources
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FeedbackContentCreator(context: Context, feedback: Feedback) {
    private var sectionTitleBuilder = StringBuilder()
    private var sectionTextBuilder = StringBuilder()
    init {
        // Localization is not needed here
        // because this ends up on a page where developers read the feedback,
        // so English is the most convenient.

        //Get the UTC Date and Time and add it to the Title
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val utcFormattedDate = dateFormat.format(Date())

        // Construct the feedback section title
        sectionTitleBuilder.append("Feedback from  ")
        sectionTitleBuilder.append(getUserName(context))
        sectionTitleBuilder.append(" for version ")
        sectionTitleBuilder.append(feedback.version)
        sectionTitleBuilder.append(" on ")
        sectionTitleBuilder.append(utcFormattedDate)
        
        // Construct the feedback section text
        sectionTextBuilder = StringBuilder()
        sectionTextBuilder.append("\n")
        sectionTextBuilder.append(feedback.title)
        sectionTextBuilder.append("\n")
        sectionTextBuilder.append("\n")
        if (feedback.apiLevel != null) {
            sectionTextBuilder.append("* ")
            sectionTextBuilder.append(
                getLocalizedResources(
                    context,
                    Locale.ENGLISH
                ).getString(R.string.api_level)
            )
            sectionTextBuilder.append(": ")
            sectionTextBuilder.append(feedback.apiLevel)
            sectionTextBuilder.append("\n")
        }
        if (feedback.androidVersion != null) {
            sectionTextBuilder.append("* ")
            sectionTextBuilder.append(
                getLocalizedResources(
                    context,
                    Locale.ENGLISH
                ).getString(R.string.android_version)
            )
            sectionTextBuilder.append(": ")
            sectionTextBuilder.append(feedback.androidVersion)
            sectionTextBuilder.append("\n")
        }
        if (feedback.deviceManufacturer != null) {
            sectionTextBuilder.append("* ")
            sectionTextBuilder.append(
                getLocalizedResources(
                    context,
                    Locale.ENGLISH
                ).getString(R.string.device_manufacturer)
            )
            sectionTextBuilder.append(": ")
            sectionTextBuilder.append(feedback.deviceManufacturer)
            sectionTextBuilder.append("\n")
        }
        if (feedback.deviceModel != null) {
            sectionTextBuilder.append("* ")
            sectionTextBuilder.append(
                getLocalizedResources(
                    context,
                    Locale.ENGLISH
                ).getString(R.string.device_model)
            )
            sectionTextBuilder.append(": ")
            sectionTextBuilder.append(feedback.deviceModel)
            sectionTextBuilder.append("\n")
        }
        if (feedback.device != null) {
            sectionTextBuilder.append("* ")
            sectionTextBuilder.append(
                getLocalizedResources(
                    context,
                    Locale.ENGLISH
                ).getString(R.string.device_name)
            )
            sectionTextBuilder.append(": ")
            sectionTextBuilder.append(feedback.device)
            sectionTextBuilder.append("\n")
        }
        if (feedback.networkType != null) {
            sectionTextBuilder.append("* ")
            sectionTextBuilder.append(
                getLocalizedResources(
                    context,
                    Locale.ENGLISH
                ).getString(R.string.network_type)
            )
            sectionTextBuilder.append(": ")
            sectionTextBuilder.append(feedback.networkType)
            sectionTextBuilder.append("\n")
        }
        sectionTextBuilder.append("~~~~")
        sectionTextBuilder.append("\n")
    }

    fun getSectionText(): String {
        return sectionTextBuilder.toString()
    }

    fun getSectionTitle(): String {
        return sectionTitleBuilder.toString()
    }
}