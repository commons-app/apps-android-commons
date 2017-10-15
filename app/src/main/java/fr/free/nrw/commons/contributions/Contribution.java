package fr.free.nrw.commons.contributions;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.settings.Prefs;

public class Contribution extends Media {

    public static Creator<Contribution> CREATOR = new Creator<Contribution>() {
        @Override
        public Contribution createFromParcel(Parcel parcel) {
            return new Contribution(parcel);
        }

        @Override
        public Contribution[] newArray(int i) {
            return new Contribution[0];
        }
    };

    // No need to be bitwise - they're mutually exclusive
    public static final int STATE_COMPLETED = -1;
    public static final int STATE_FAILED = 1;
    public static final int STATE_QUEUED = 2;
    public static final int STATE_IN_PROGRESS = 3;

    public static final String SOURCE_CAMERA = "camera";
    public static final String SOURCE_GALLERY = "gallery";
    public static final String SOURCE_EXTERNAL = "external";

    private Uri contentUri;
    private String source;
    private String editSummary;
    private Date timestamp;
    private int state;
    private long transferred;
    private String decimalCoords;
    private boolean isMultiple;

    public Contribution(Uri contentUri, String filename, Uri localUri, String imageUrl, Date timestamp,
                        int state, long dataLength, Date dateUploaded, long transferred,
                        String source, String description, String creator, boolean isMultiple,
                        int width, int height, String license) {
        super(localUri, imageUrl, filename, description, dataLength, timestamp, dateUploaded, creator);
        this.contentUri = contentUri;
        this.state = state;
        this.timestamp = timestamp;
        this.transferred = transferred;
        this.source = source;
        this.isMultiple = isMultiple;
        this.width = width;
        this.height = height;
        this.license = license;
    }

    public Contribution(Uri localUri, String imageUrl, String filename, String description, long dataLength,
                        Date dateCreated, Date dateUploaded, String creator, String editSummary, String decimalCoords) {
        super(localUri, imageUrl, filename, description, dataLength, dateCreated, dateUploaded, creator);
        this.decimalCoords = decimalCoords;
        this.editSummary = editSummary;
        timestamp = new Date(System.currentTimeMillis());
    }

    public Contribution(Parcel in) {
        super(in);
        contentUri = in.readParcelable(Uri.class.getClassLoader());
        source = in.readString();
        timestamp = (Date) in.readSerializable();
        state = in.readInt();
        transferred = in.readLong();
        isMultiple = in.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeParcelable(contentUri, flags);
        parcel.writeString(source);
        parcel.writeSerializable(timestamp);
        parcel.writeInt(state);
        parcel.writeLong(transferred);
        parcel.writeInt(isMultiple ? 1 : 0);
    }

    public boolean getMultiple() {
        return isMultiple;
    }

    public void setMultiple(boolean multiple) {
        isMultiple = multiple;
    }

    public long getTransferred() {
        return transferred;
    }

    public void setTransferred(long transferred) {
        this.transferred = transferred;
    }

    public String getEditSummary() {
        return editSummary != null ? editSummary : CommonsApplication.DEFAULT_EDIT_SUMMARY;
    }

    public Uri getContentUri() {
        return contentUri;
    }

    public void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setDateUploaded(Date date) {
        this.dateUploaded = date;
    }

    public String getTrackingTemplates() {
        return "{{subst:unc}}";  // Remove when we have categorization
    }

    public String getPageContents() {
        StringBuilder buffer = new StringBuilder();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        buffer
                .append("== {{int:filedesc}} ==\n")
                .append("{{Information\n")
                .append("|description=").append(getDescription()).append("\n")
                .append("|source=").append("{{own}}\n")
                .append("|author=[[User:").append(creator).append("|").append(creator).append("]]\n");
        if (dateCreated != null) {
            buffer
                    .append("|date={{According to EXIF data|").append(isoFormat.format(dateCreated)).append("}}\n");
        }
        buffer
                .append("}}").append("\n");

        //Only add Location template (e.g. {{Location|37.51136|-77.602615}} ) if coords is not null
        if (decimalCoords != null) {
            buffer.append("{{Location|").append(decimalCoords).append("}}").append("\n");
        }

        buffer.append("== {{int:license-header}} ==\n")
                .append(licenseTemplateFor(getLicense())).append("\n\n")
                .append("{{Uploaded from Mobile|platform=Android|version=").append(BuildConfig.VERSION_NAME).append("}}\n")
                .append(getTrackingTemplates());
        return buffer.toString();
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Contribution() {
        timestamp = new Date(System.currentTimeMillis());
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setLocalUri(Uri localUri) {
        this.localUri = localUri;
    }

    public void setDecimalCoords(String decimalCoords) {
        this.decimalCoords = decimalCoords;
    }

    @NonNull
    private String licenseTemplateFor(String license) {
        switch (license) {
            case Prefs.Licenses.CC_BY_3:
                return "{{self|cc-by-3.0}}";
            case Prefs.Licenses.CC_BY_4:
                return "{{self|cc-by-4.0}}";
            case Prefs.Licenses.CC_BY_SA_3:
                return "{{self|cc-by-sa-3.0}}";
            case Prefs.Licenses.CC_BY_SA_4:
                return "{{self|cc-by-sa-4.0}}";
            case Prefs.Licenses.CC0:
                return "{{self|cc-zero}}";
            case Prefs.Licenses.CC_BY:
                return "{{self|cc-by-3.0}}";
            case Prefs.Licenses.CC_BY_SA:
                return "{{self|cc-by-sa-3.0}}";
        }

        throw new RuntimeException("Unrecognized license value: " + license);
    }
}
