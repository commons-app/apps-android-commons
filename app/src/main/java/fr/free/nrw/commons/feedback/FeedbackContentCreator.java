package fr.free.nrw.commons.feedback;

import android.content.Context;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.feedback.model.Feedback;
import fr.free.nrw.commons.utils.LangCodeUtils;
import java.util.Locale;

/**
 * Creates a wikimedia recognizable format
 * from feedback information
 */
public class FeedbackContentCreator {
    private StringBuilder sectionTextBuilder;
    private StringBuilder sectionTitleBuilder;
    private Feedback feedback;
    private Context context;

    public FeedbackContentCreator(Context context, Feedback feedback) {
        this.feedback = feedback;
        this.context = context;
        init();
    }

    /**
     * Initializes the string buffer object to append content from feedback object
     */
    public void init() {
        // Localization is not needed here, because this ends up on a page where developers read the feedback, so English is the most convenient.

        /*
         * Construct the feedback section title
         */
        sectionTitleBuilder = new StringBuilder();
        sectionTitleBuilder.append("Feedback from  ");
        sectionTitleBuilder.append(AccountUtil.getUserName(context));
        sectionTitleBuilder.append(" for version ");
        sectionTitleBuilder.append(feedback.getVersion());

        /*
         * Construct the feedback section text
         */
        sectionTextBuilder = new StringBuilder();
        sectionTextBuilder.append("\n");
        sectionTextBuilder.append(feedback.getTitle());
        sectionTextBuilder.append("\n");
        sectionTextBuilder.append("\n");
        if (feedback.getApiLevel() != null) {
            sectionTextBuilder.append("* ");
            sectionTextBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.api_level));
            sectionTextBuilder.append(": ");
            sectionTextBuilder.append(feedback.getApiLevel());
            sectionTextBuilder.append("\n");
        }
        if (feedback.getAndroidVersion() != null) {
            sectionTextBuilder.append("* ");
            sectionTextBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.android_version));
            sectionTextBuilder.append(": ");
            sectionTextBuilder.append(feedback.getAndroidVersion());
            sectionTextBuilder.append("\n");
        }
        if (feedback.getDeviceManufacturer() != null) {
            sectionTextBuilder.append("* ");
            sectionTextBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.device_manufacturer));
            sectionTextBuilder.append(": ");
            sectionTextBuilder.append(feedback.getDeviceManufacturer());
            sectionTextBuilder.append("\n");
        }
        if (feedback.getDeviceModel() != null) {
            sectionTextBuilder.append("* ");
            sectionTextBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.device_model));
            sectionTextBuilder.append(": ");
            sectionTextBuilder.append(feedback.getDeviceModel());
            sectionTextBuilder.append("\n");
        }
        if (feedback.getDevice() != null) {
            sectionTextBuilder.append("* ");
            sectionTextBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.device_name));
            sectionTextBuilder.append(": ");
            sectionTextBuilder.append(feedback.getDevice());
            sectionTextBuilder.append("\n");
        }
        if (feedback.getNetworkType() != null) {
            sectionTextBuilder.append("* ");
            sectionTextBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.network_type));
            sectionTextBuilder.append(": ");
            sectionTextBuilder.append(feedback.getNetworkType());
            sectionTextBuilder.append("\n");
        }
        sectionTextBuilder.append("~~~~");
        sectionTextBuilder.append("\n");
    }

    public String getSectionText() {
        return sectionTextBuilder.toString();
    }

    public String getSectionTitle() {
        return sectionTitleBuilder.toString();
    }
}
