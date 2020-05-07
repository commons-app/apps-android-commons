package fr.free.nrw.commons.contributions;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.upload.WikidataPlace;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "contribution")
public class Contribution extends Media {

    // No need to be bitwise - they're mutually exclusive
    public static final int STATE_COMPLETED = -1;
    public static final int STATE_FAILED = 1;
    public static final int STATE_QUEUED = 2;
    public static final int STATE_IN_PROGRESS = 3;

    private int state;
    private long transferred;
    private String decimalCoords;
    private String dateCreatedSource;
    private WikidataPlace wikidataPlace;
    /**
     * Each depiction loaded in depictions activity is associated with a wikidata entity id, this Id
     * is in turn used to upload depictions to wikibase
     */
    private List<DepictedItem> depictedItems = new ArrayList<>();
    private String mimeType;
    /**
     * This hasmap stores the list of multilingual captions, where key of the HashMap is the language
     * and value is the caption in the corresponding language Ex: key = "en", value: "<caption in
     * short in English>" key = "de" , value: "<caption in german>"
     */
    private Map<String, String> captions = new HashMap<>();

    public Contribution() {
    }

    public Contribution(final UploadItem item, final SessionManager sessionManager,
        final List<DepictedItem> depictedItems, final List<String> categories) {
        super(item.getMediaUri(),
            item.getFileName(),
            UploadMediaDetail.formatList(item.getUploadMediaDetails()),
            sessionManager.getAuthorName(),
            categories);
        captions = UploadMediaDetail.formatCaptions(item.getUploadMediaDetails());
        decimalCoords = item.getGpsCoords().getDecimalCoords();
        dateCreatedSource = "";
        this.depictedItems = depictedItems;
        wikidataPlace = WikidataPlace.from(item.getPlace());
    }

    public void setDateCreatedSource(final String dateCreatedSource) {
        this.dateCreatedSource = dateCreatedSource;
    }

    public String getDateCreatedSource() {
        return dateCreatedSource;
    }

    public long getTransferred() {
        return transferred;
    }

    public void setTransferred(final long transferred) {
        this.transferred = transferred;
    }

    public int getState() {
        return state;
    }

    public void setState(final int state) {
        this.state = state;
    }

    /**
     * @return array list of entityids for the depictions
     */
    public List<DepictedItem> getDepictedItems() {
        return depictedItems;
    }

    public void setWikidataPlace(final WikidataPlace wikidataPlace) {
        this.wikidataPlace = wikidataPlace;
    }

    public WikidataPlace getWikidataPlace() {
        return wikidataPlace;
    }

    public String getDecimalCoords() {
        return decimalCoords;
    }

    public void setDecimalCoords(final String decimalCoords) {
        this.decimalCoords = decimalCoords;
    }

    public void setDepictedItems(final List<DepictedItem> depictedItems) {
        this.depictedItems = depictedItems;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Captions are a feature part of Structured data. They are meant to store short, multilingual
     * descriptions about files This is a replacement of the previously used titles for images (titles
     * were not multilingual) Also now captions replace the previous convention of using title for
     * filename
     * <p>
     * key of the HashMap is the language and value is the caption in the corresponding language
     * <p>
     * returns list of captions stored in hashmap
     */
    public Map<String, String> getCaptions() {
        return captions;
    }

    public void setCaptions(Map<String, String> captions) {
        this.captions = captions;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(state);
        dest.writeLong(transferred);
        dest.writeString(decimalCoords);
        dest.writeString(dateCreatedSource);
        dest.writeSerializable((HashMap) captions);
    }

    public Contribution(Media media, int state) {
        super(media.getPageId(),
            media.getLocalUri(), media.getThumbUrl(), media.getImageUrl(), media.getFilename(),
            media.getDescription(),
            media.getDiscussion(),
            media.getDataLength(), media.getDateCreated(), media.getDateUploaded(),
            media.getLicense(), media.getLicenseUrl(), media.getCreator(), media.getCategories(),
            media.isRequestedDeletion(), media.getCoordinates());
        this.state = state;
    }

    protected Contribution(final Parcel in) {
        super(in);
        state = in.readInt();
        transferred = in.readLong();
        decimalCoords = in.readString();
        dateCreatedSource = in.readString();
        captions = (HashMap<String, String>) in.readSerializable();
    }

    public static final Creator<Contribution> CREATOR = new Creator<Contribution>() {
        @Override
        public Contribution createFromParcel(final Parcel source) {
            return new Contribution(source);
        }

        @Override
        public Contribution[] newArray(final int size) {
            return new Contribution[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return getPageId();
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        return obj.toString() == toString();
    }
}
