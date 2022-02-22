package fr.free.nrw.commons.feedback;

import android.content.Context;
import fr.free.nrw.commons.feedback.Feedback;

public class FeedbackContentCreator {
    private Context context;
    private StringBuilder stringBuilder;
    private Feedback feedback;

    public FeedbackContentCreator(final Context context, Feedback feedback) {
        this.context = context;
        this.feedback = feedback;
        init();
    }

    /**
     * Initializes the string buffer object to append content from feedback object
     */
    public void init() {
        stringBuilder = new StringBuilder();
        stringBuilder.append("== ").append("Feedback for version").append(feedback.getVersion()).append(" ==");
        stringBuilder.append("\n");
        stringBuilder.append(feedback.getTitle());
        stringBuilder.append("\n");
        stringBuilder.append("\n");
        if(feedback.getApiLevel()!=null) {
            stringBuilder.append("* ").append("API level: ").append(feedback.getApiLevel());
            stringBuilder.append("\n");
        }
        if(feedback.getAndroidVersion()!=null) {
            stringBuilder.append("* ").append("Android Version: ")
                .append(feedback.getAndroidVersion());
            stringBuilder.append("\n");
        }
        if(feedback.getDeviceManufacturer()!=null) {
            stringBuilder.append("* ").append("Device manufacturer: ")
                .append(feedback.getDeviceManufacturer());
            stringBuilder.append("\n");
        }
        if(feedback.getDeviceModel()!=null) {
            stringBuilder.append("* ").append("Device model: ").append(feedback.getDeviceModel());
            stringBuilder.append("\n");
        }
        if(feedback.getDevice()!=null) {
            stringBuilder.append("* ").append("Device: ").append(feedback.getDevice());
            stringBuilder.append("\n");
        }
        if(feedback.getNetworkType()!=null) {
            stringBuilder.append("* ").append("Network type: ").append(feedback.getNetworkType());
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
