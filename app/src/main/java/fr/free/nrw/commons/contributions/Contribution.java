package fr.free.nrw.commons.contributions;

import android.os.Parcel;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.upload.WikidataPlace;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.wikipedia.dataclient.mwapi.MwQueryLogEvent;

@Entity(tableName = "contribution")
public class Contribution extends Media {

    // No need to be bitwise - they're mutually exclusive
    public static final int STATE_COMPLETED = -1;
    public static final int STATE_FAILED = 1;
    public static final int STATE_QUEUED = 2;
    public static final int STATE_IN_PROGRESS = 3;

    @PrimaryKey (autoGenerate = true)
    private long _id;
    private int state;
    private long transferred;
    private String decimalCoords;
    private String dateCreatedSource;
    private WikidataPlace wikidataPlace;
    /**
     * Each depiction loaded in depictions activity is associated with a wikidata entity id,
     * this Id is in turn used to upload depictions to wikibase
     */
    private List<DepictedItem> depictedItems = new ArrayList<>();
    private String mimeType;

    public Contribution() {
    }

    public Contribution(final UploadItem item, final SessionManager sessionManager,
        final List<DepictedItem> depictedItems,
        final Collection<String> categories) {
        super(item.getMediaUri(), null, item.getFileName(), UploadMediaDetail.formatCaptions(item.getUploadMediaDetails()), UploadMediaDetail.formatList(item.getUploadMediaDetails()), -1, null, new Date(), sessionManager.getAuthorName());
        decimalCoords = item.getGpsCoords().getDecimalCoords();
        dateCreatedSource = "";
        this.depictedItems.addAll(depictedItems);
        wikidataPlace = WikidataPlace.from(item.getPlace());
        this.categories.addAll(categories);
    }

    public Contribution(final MwQueryLogEvent queryLogEvent, final String user) {
        super(null, null, queryLogEvent.title(), new HashMap<>(), "", -1, queryLogEvent.date(), queryLogEvent.date(), user);
        decimalCoords = "";
        dateCreatedSource = "";
        state = STATE_COMPLETED;
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

    public void setDateUploaded(final Date date) {
        dateUploaded = date;
    }

    @Override
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public void setImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
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

    public long get_id() {
        return _id;
    }

    public void set_id(final long _id) {
        this._id = _id;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(_id);
        dest.writeInt(state);
        dest.writeLong(transferred);
        dest.writeString(decimalCoords);
        dest.writeString(dateCreatedSource);
    }

    protected Contribution(final Parcel in) {
        super(in);
        _id = in.readLong();
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

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
