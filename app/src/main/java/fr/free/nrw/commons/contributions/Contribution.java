package fr.free.nrw.commons.contributions;

import android.net.Uri;
import android.os.Parcel;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.upload.UploadItem;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.WikidataPlace;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
    @Nullable
    private Uri localUri;
    private long dataLength;
    private Date dateCreated;

    public Contribution() {
    }

    public Contribution(final UploadItem item, final SessionManager sessionManager,
        final List<DepictedItem> depictedItems, final List<String> categories) {
        super(
            item.getFileName(),
            UploadMediaDetail.formatCaptions(item.getUploadMediaDetails()),
            UploadMediaDetail.formatDescriptions(item.getUploadMediaDetails()),
            sessionManager.getAuthorName(),
            categories);
        localUri = item.getMediaUri();
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
    }

    /**
     * Constructor that takes Media object and state as parameters and builds a new Contribution object
     * @param media
     * @param state
     */
    public Contribution(Media media, int state) {
        super(media);
        this.state = state;
    }

    protected Contribution(final Parcel in) {
        super(in);
        state = in.readInt();
        transferred = in.readLong();
        decimalCoords = in.readString();
        dateCreatedSource = in.readString();
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

    @Nullable
    public Uri getLocalUri() {
        return localUri;
    }

    public void setLocalUri(@Nullable Uri localUri) {
        this.localUri = localUri;
    }

    public long getDataLength() {
        return dataLength;
    }

    public void setDataLength(long dataLength) {
        this.dataLength = dataLength;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final Contribution that = (Contribution) o;
        return state == that.state &&
            transferred == that.transferred &&
            dataLength == that.dataLength &&
            Objects.equals(decimalCoords, that.decimalCoords) &&
            Objects.equals(dateCreatedSource, that.dateCreatedSource) &&
            Objects.equals(wikidataPlace, that.wikidataPlace) &&
            Objects.equals(depictedItems, that.depictedItems) &&
            Objects.equals(mimeType, that.mimeType) &&
            Objects.equals(localUri, that.localUri) &&
            Objects.equals(dateCreated, that.dateCreated);
    }

    @Override
    public int hashCode() {
        return Objects
            .hash(super.hashCode(), state, transferred, decimalCoords, dateCreatedSource,
                wikidataPlace,
                depictedItems, mimeType, localUri, dataLength, dateCreated);
    }
}
