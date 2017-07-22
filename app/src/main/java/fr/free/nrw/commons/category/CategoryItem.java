package fr.free.nrw.commons.category;

import android.os.Parcel;
import android.os.Parcelable;

class CategoryItem implements Parcelable {
    public final String name;
    public boolean selected;

    public static Creator<CategoryItem> CREATOR = new Creator<CategoryItem>() {
        @Override
        public CategoryItem createFromParcel(Parcel parcel) {
            return new CategoryItem(parcel);
        }

        @Override
        public CategoryItem[] newArray(int i) {
            return new CategoryItem[0];
        }
    };

    CategoryItem(String name, boolean selected) {
        this.name = name;
        this.selected = selected;
    }

    private CategoryItem(Parcel in) {
        name = in.readString();
        selected = in.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(name);
        parcel.writeInt(selected ? 1 : 0);
    }
}
