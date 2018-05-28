package fr.free.nrw.commons.explore.images;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchImageItem implements Parcelable {
    private final String name;
    private boolean image_stored_in_history;

    public static Creator<SearchImageItem> CREATOR = new Creator<SearchImageItem>() {
        @Override
        public SearchImageItem createFromParcel(Parcel parcel) {
            return new SearchImageItem(parcel);
        }

        @Override
        public SearchImageItem[] newArray(int i) {
            return new SearchImageItem[0];
        }
    };

    public SearchImageItem(String name, boolean image_stored_in_history) {
        this.name = name;
        this.image_stored_in_history = image_stored_in_history;
    }

    private SearchImageItem(Parcel in) {
        name = in.readString();
        image_stored_in_history = in.readInt() == 1;
    }

    public String getName() {
        return name;
    }

    public boolean isImage_stored_in_history() {
        return image_stored_in_history;
    }

    public void setImage_stored_in_history(boolean image_stored_in_history) {
        this.image_stored_in_history = image_stored_in_history;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(name);
        parcel.writeInt(image_stored_in_history ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SearchImageItem that = (SearchImageItem) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
