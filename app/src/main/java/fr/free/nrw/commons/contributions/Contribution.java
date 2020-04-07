package fr.free.nrw.commons.contributions;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.filepicker.UploadableFile.DateTimeWithSource;
import fr.free.nrw.commons.settings.Prefs.Licenses;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.upload.WikidataPlace;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.utils.ConfigUtils;
import java.lang.annotation.Retention;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

@Entity(tableName = "contribution")
public class Contribution extends Media {

    //{{According to Exif data|2009-01-09}}
    private static final String TEMPLATE_DATE_ACC_TO_EXIF = "{{According to Exif data|%s}}";

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
    
    @PrimaryKey (autoGenerate = true)
    @NonNull
    private long _id;
    private String source;
    private String editSummary;
    private int state;
    private long transferred;
    private String decimalCoords;
    private Uri contentProviderUri;
    private String dateCreatedSource;
    private WikidataPlace wikidataPlace;

    /**
     * Each depiction loaded in depictions activity is associated with a wikidata entity id,
     * this Id is in turn used to upload depictions to wikibase
     */
    private List<DepictedItem> depictedItems = new ArrayList<>();

    public Contribution(UploadItem item, SessionManager sessionManager,
        List<DepictedItem> depictedItems,
        Collection<String> categories) {
        super(item.getMediaUri(), null, item.getFileName(), UploadMediaDetail.formatCaptions(item.getUploadMediaDetails()), UploadMediaDetail.formatList(item.getUploadMediaDetails()), -1, null, new Date(), sessionManager.getAuthorName());
        this.decimalCoords = item.getGpsCoords().getDecimalCoords();
        this.editSummary = CommonsApplication.DEFAULT_EDIT_SUMMARY;
        this.dateCreatedSource = "";
        this.depictedItems.addAll(depictedItems);
        this.wikidataPlace = WikidataPlace.from(item.getPlace());
        this.categories.addAll(categories);
        this.source = item.getSource();
        this.contentProviderUri = item.getMediaUri();
    }

    public Contribution(Uri localUri, String imageUrl, String filename, Map<String, String> captions, String description, long dataLength,
                        Date dateCreated, Date dateUploaded, String creator, String editSummary, String decimalCoords, int state) {
        super(localUri, imageUrl, filename, captions, description, dataLength, dateCreated, dateUploaded, creator);
        this.decimalCoords = decimalCoords;
        this.editSummary = editSummary;
        this.dateCreatedSource = "";
        this.state=state;
    }

    public void setDateCreatedSource(String dateCreatedSource) {
        this.dateCreatedSource = dateCreatedSource;
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
        else {
            buffer.append("{{subst:unc}}");
        }
        return buffer.toString();
    }

    /**
     * Returns upload date in either TEMPLATE_DATE_ACC_TO_EXIF or TEMPLATE_DATA_OTHER_SOURCE
     * @return
     */
    private String getTemplatizedCreatedDate() {
        if (dateCreated != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            if (DateTimeWithSource.EXIF_SOURCE.equals(dateCreatedSource)) {
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

    @NonNull
    private String licenseTemplateFor(String license) {
        switch (license) {
            case Licenses.CC_BY_3:
                return "{{self|cc-by-3.0}}";
            case Licenses.CC_BY_4:
                return "{{self|cc-by-4.0}}";
            case Licenses.CC_BY_SA_3:
                return "{{self|cc-by-sa-3.0}}";
            case Licenses.CC_BY_SA_4:
                return "{{self|cc-by-sa-4.0}}";
            case Licenses.CC0:
                return "{{self|cc-zero}}";
        }

        throw new RuntimeException("Unrecognized license value: " + license);
    }

    public void setContentProviderUri(Uri contentProviderUri) {
        this.contentProviderUri = contentProviderUri;
    }

    /**
     * @return array list of entityids for the depictions
     */
    public List<DepictedItem> getDepictedItems() {
        return depictedItems;
    }

    public void setWikidataPlace(WikidataPlace wikidataPlace) {
        this.wikidataPlace = wikidataPlace;
    }

    public WikidataPlace getWikidataPlace() {
        return wikidataPlace;
    }

    public long get_id() {
        return _id;
    }

    public void set_id(final long _id) {
        this._id = _id;
    }

    public void setEditSummary(final String editSummary) {
        this.editSummary = editSummary;
    }

    public String getDecimalCoords() {
        return decimalCoords;
    }

    public void setDecimalCoords(final String decimalCoords) {
        this.decimalCoords = decimalCoords;
    }

    public Uri getContentProviderUri() {
        return contentProviderUri;
    }

    public String getDateCreatedSource() {
        return dateCreatedSource;
    }

    public void setDepictedItems(final List<DepictedItem> depictedItems) {
        this.depictedItems = depictedItems;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(this._id);
        dest.writeString(this.source);
        dest.writeString(this.editSummary);
        dest.writeInt(this.state);
        dest.writeLong(this.transferred);
        dest.writeString(this.decimalCoords);
        dest.writeParcelable(this.contentProviderUri, flags);
        dest.writeString(this.dateCreatedSource);
    }

    protected Contribution(Parcel in) {
        super(in);
        this._id = in.readLong();
        this.source = in.readString();
        this.editSummary = in.readString();
        this.state = in.readInt();
        this.transferred = in.readLong();
        this.decimalCoords = in.readString();
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
