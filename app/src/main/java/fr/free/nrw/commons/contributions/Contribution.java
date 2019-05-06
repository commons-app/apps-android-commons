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

    //{{date|2009|1|9}} → 9 January 2009
    private static final String TEMPLATE_DATA_OTHER_SOURCE = "{{date|%d|%d|%d}}";

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

    public Contribution(Parcel in) {
        super(in);
        contentUri = in.readParcelable(Uri.class.getClassLoader());
        source = in.readString();
        state = in.readInt();
        transferred = in.readLong();
        isMultiple = in.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeParcelable(contentUri, flags);
        parcel.writeString(source);
        parcel.writeInt(state);
        parcel.writeLong(transferred);
        parcel.writeInt(isMultiple ? 1 : 0);
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
            if (UploadableFile.DateTimeWithSource.EXIF_SOURCE.equals(dateCreatedSource)) {
                return String.format(Locale.ENGLISH, TEMPLATE_DATE_ACC_TO_EXIF, DateUtil.getDateStringWithSkeletonPattern(dateCreated, "yyyy-MM-dd")) + "\n";
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateCreated);
                calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
                return String.format(Locale.ENGLISH, TEMPLATE_DATA_OTHER_SOURCE,
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)) + "\n";
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
}
