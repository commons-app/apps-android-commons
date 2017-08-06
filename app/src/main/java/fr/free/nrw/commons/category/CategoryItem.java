package fr.free.nrw.commons.category;

import android.os.Parcel;
import android.os.Parcelable;

class CategoryItem implements Parcelable {
    private final String name;
    private boolean selected;

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

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CategoryItem that = (CategoryItem) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
