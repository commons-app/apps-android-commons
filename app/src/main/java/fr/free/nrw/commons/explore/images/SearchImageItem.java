package fr.free.nrw.commons.explore.images;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchImageItem implements Parcelable {
    private final String name;

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

    public SearchImageItem(String name) {
        this.name = name;
    }

    private SearchImageItem(Parcel in) {
        name = in.readString();
    }

    public String getName() {
        return name;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(name);
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
