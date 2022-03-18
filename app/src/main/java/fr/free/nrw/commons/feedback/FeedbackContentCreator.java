package fr.free.nrw.commons.feedback;

import android.content.Context;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.feedback.model.Feedback;
import fr.free.nrw.commons.utils.LangCodeUtils;
import java.util.Locale;

/**
 * Creates a wikimedia recognizable format
 * from feedback information
 */
public class FeedbackContentCreator {
    private StringBuilder stringBuilder;
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

        stringBuilder = new StringBuilder();
        stringBuilder.append("== ");
        stringBuilder.append("Feedback for version ");
        stringBuilder.append(feedback.getVersion());
        stringBuilder.append(" ==");
        stringBuilder.append("\n");
        stringBuilder.append(feedback.getTitle());
        stringBuilder.append("\n");
        stringBuilder.append("\n");
        if (feedback.getApiLevel() != null) {
            stringBuilder.append("* ");
            stringBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.api_level));
            stringBuilder.append(": ");
            stringBuilder.append(feedback.getApiLevel());
            stringBuilder.append("\n");
        }
        if (feedback.getAndroidVersion() != null) {
            stringBuilder.append("* ");
            stringBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.android_version));
            stringBuilder.append(": ");
            stringBuilder.append(feedback.getAndroidVersion());
            stringBuilder.append("\n");
        }
        if (feedback.getDeviceManufacturer() != null) {
            stringBuilder.append("* ");
            stringBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.device_manufacturer));
            stringBuilder.append(": ");
            stringBuilder.append(feedback.getDeviceManufacturer());
            stringBuilder.append("\n");
        }
        if (feedback.getDeviceModel() != null) {
            stringBuilder.append("* ");
            stringBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.device_model));
            stringBuilder.append(": ");
            stringBuilder.append(feedback.getDeviceModel());
            stringBuilder.append("\n");
        }
        if (feedback.getDevice() != null) {
            stringBuilder.append("* ");
            stringBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.device_name));
            stringBuilder.append(": ");
            stringBuilder.append(feedback.getDevice());
            stringBuilder.append("\n");
        }
        if (feedback.getNetworkType() != null) {
            stringBuilder.append("* ");
            stringBuilder.append(LangCodeUtils.getLocalizedResources(context,
                Locale.ENGLISH).getString(R.string.network_type));
            stringBuilder.append(": ");
            stringBuilder.append(feedback.getNetworkType());
            stringBuilder.append("\n");
        }
        stringBuilder.append("~~~~");
        stringBuilder.append("\n");
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
