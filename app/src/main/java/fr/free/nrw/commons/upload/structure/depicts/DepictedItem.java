package fr.free.nrw.commons.upload.structure.depicts;

import android.os.Parcel;
import android.os.Parcelable;

public class DepictedItem implements Parcelable {
    private final String name;
    private boolean selected;

    public DepictedItem(String name, boolean selected) {
        this.name = name;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    protected DepictedItem(Parcel in) {
        name = in.readString();
        selected = in.readByte() != 0;
    }

    public static final Creator<DepictedItem> CREATOR = new Creator<DepictedItem>() {
        @Override
        public DepictedItem createFromParcel(Parcel in) {
            return new DepictedItem(in);
        }

        @Override
        public DepictedItem[] newArray(int size) {
            return new DepictedItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeByte((byte) (selected ? 1 : 0));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DepictedItem that = (DepictedItem) o;

        return name.equals(that.name);
    }
}
