package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.util.DateUtil;

import java.lang.annotation.Retention;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.ConfigUtils;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class  Contribution extends Media {

    //{{According to EXIF data|2009-01-09}}
    private static final String TEMPLATE_DATE_ACC_TO_EXIF = "{{According to EXIF data|%s}}";

    //2009-01-09 → 9 January 2009
    private static final String TEMPLATE_DATA_OTHER_SOURCE = "%s";

    // No need to be bitwise - they're mutually exclusive
    public static final int STATE_COMPLETED = -1;
    public static final int STATE_FAILED = 1;
    public static final int STATE_QUEUED = 2;
    public static final int STATE_IN_PROGRESS = 3;

    @Retention(SOURCE)
    @StringDef({SOURCE_CAMERA, SOURCE_GALLERY, SOURCE_EXTERNAL})
    public @interface FileSource {}

    public static final String SOURCE_CAMERA = "camera";
    public static final String SOURCE_GALLERY = "gallery";
    public static final String SOURCE_EXTERNAL = "external";

    private Uri contentUri;
    private String source;
    private String editSummary;
    private int state;
    private long transferred;
    private String decimalCoords;
    private boolean isMultiple;
    private String wikiDataEntityId;
    private Uri contentProviderUri;
    private String dateCreatedSource;

    public Contribution(Uri contentUri, String filename, Uri localUri, String imageUrl, Date dateCreated,
                        int state, long dataLength, Date dateUploaded, long transferred,
                        String source, String description, String creator, boolean isMultiple,
                        int width, int height, String license) {
        super(localUri, imageUrl, filename, description, dataLength, dateCreated, dateUploaded, creator);
        this.contentUri = contentUri;
        this.state = state;
        this.transferred = transferred;
        this.source = source;
        this.isMultiple = isMultiple;
        this.width = width;
        this.height = height;
        this.license = license;
        this.dateCreatedSource = "";
    }

    public Contribution(Uri localUri, String imageUrl, String filename, String description, long dataLength,
                        Date dateCreated, Date dateUploaded, String creator, String editSummary, String decimalCoords) {
        super(localUri, imageUrl, filename, description, dataLength, dateCreated, dateUploaded, creator);
        this.decimalCoords = decimalCoords;
        this.editSummary = editSummary;
        this.dateCreatedSource = "";
    }

    public Contribution(Uri localUri, String imageUrl, String filename, String description, long dataLength,
                        Date dateCreated, Date dateUploaded, String creator, String editSummary, String decimalCoords, int state) {
        super(localUri, imageUrl, filename, description, dataLength, dateCreated, dateUploaded, creator);
        this.decimalCoords = decimalCoords;
        this.editSummary = editSummary;
        this.dateCreatedSource = "";
        this.state=state;
    }

    public String getDateCreatedSource() {
        return dateCreatedSource;
    }

    public void setDateCreatedSource(String dateCreatedSource) {
        this.dateCreatedSource = dateCreatedSource;
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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setDateUploaded(Date date) {
        this.dateUploaded = date;
    }

    public String getPageContents(Context applicationContext) {
        StringBuilder buffer = new StringBuilder();
        buffer
                .append("== {{int:filedesc}} ==\n")
                .append("{{Information\n")
                .append("|description=").append(getDescription()).append("\n")
                .append("|source=").append("{{own}}\n")
                .append("|author=[[User:").append(creator).append("|").append(creator).append("]]\n");

        String templatizedCreatedDate = getTemplatizedCreatedDate();
        if (!StringUtils.isBlank(templatizedCreatedDate)) {
            buffer.append("|date=").append(templatizedCreatedDate);
        }

        buffer.append("}}").append("\n");

        //Only add Location template (e.g. {{Location|37.51136|-77.602615}} ) if coords is not null
        if (decimalCoords != null) {
            buffer.append("{{Location|").append(decimalCoords).append("}}").append("\n");
        }

        buffer.append("== {{int:license-header}} ==\n")
                .append(licenseTemplateFor(getLicense())).append("\n\n")
                .append("{{Uploaded from Mobile|platform=Android|version=")
                .append(ConfigUtils.getVersionNameWithSha(applicationContext)).append("}}\n");
        if(categories!=null&&categories.size()!=0) {
            for (int i = 0; i < categories.size(); i++) {
                String category = categories.get(i);
                buffer.append("\n[[Category:").append(category).append("]]");
            }
        }
        else
            buffer.append("{{subst:unc}}");
        return buffer.toString();
    }

    /**
     * Returns upload date in either TEMPLATE_DATE_ACC_TO_EXIF or TEMPLATE_DATA_OTHER_SOURCE
     * @return
     */
    private String getTemplatizedCreatedDate() {
        if (dateCreated != null) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            if (UploadableFile.DateTimeWithSource.EXIF_SOURCE.equals(dateCreatedSource)) {
                return String.format(Locale.ENGLISH, TEMPLATE_DATE_ACC_TO_EXIF, dateFormat.format(dateCreated)) + "\n";
            } else {
                return String.format(Locale.ENGLISH, TEMPLATE_DATA_OTHER_SOURCE, dateFormat.format(dateCreated)) + "\n";
            }
        }
        return "";
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Contribution() {

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
        }

        throw new RuntimeException("Unrecognized license value: " + license);
    }

    public String getWikiDataEntityId() {
        return wikiDataEntityId;
    }

    /**
     * When the corresponding wikidata entity is known as in case of nearby uploads, it can be set
     * using the setter method
     * @param wikiDataEntityId wikiDataEntityId
     */
    public void setWikiDataEntityId(String wikiDataEntityId) {
        this.wikiDataEntityId = wikiDataEntityId;
    }

    public void setContentProviderUri(Uri contentProviderUri) {
        this.contentProviderUri = contentProviderUri;
    }

    public Uri getContentProviderUri() {
        return contentProviderUri;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(this.contentUri, flags);
        dest.writeString(this.source);
        dest.writeString(this.editSummary);
        dest.writeInt(this.state);
        dest.writeLong(this.transferred);
        dest.writeString(this.decimalCoords);
        dest.writeByte(this.isMultiple ? (byte) 1 : (byte) 0);
        dest.writeString(this.wikiDataEntityId);
        dest.writeParcelable(this.contentProviderUri, flags);
        dest.writeString(this.dateCreatedSource);
    }

    protected Contribution(Parcel in) {
        super(in);
        this.contentUri = in.readParcelable(Uri.class.getClassLoader());
        this.source = in.readString();
        this.editSummary = in.readString();
        this.state = in.readInt();
        this.transferred = in.readLong();
        this.decimalCoords = in.readString();
        this.isMultiple = in.readByte() != 0;
        this.wikiDataEntityId = in.readString();
        this.contentProviderUri = in.readParcelable(Uri.class.getClassLoader());
        this.dateCreatedSource = in.readString();
    }

    public static final Creator<Contribution> CREATOR = new Creator<Contribution>() {
        @Override
        public Contribution createFromParcel(Parcel source) {
            return new Contribution(source);
        }

        @Override
        public Contribution[] newArray(int size) {
            return new Contribution[size];
        }
    };
}
