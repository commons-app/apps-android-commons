package fr.free.nrw.commons.category;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a Category Item.
 * Implemented as Parcelable so that its object could be parsed between activity components.
 */
public class CategoryItem implements Parcelable {
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

    public CategoryItem(String name, boolean selected) {
        this.name = name;
        this.selected = selected;
    }

    /**
     * Reads from the received Parcel
     * @param in
     */
    private CategoryItem(Parcel in) {
        name = in.readString();
        selected = in.readInt() == 1;
    }

    /**
     * Gets Name
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if that Category Item has been selected.
     * @return
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Selects the Category Item.
     * @param selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Used by Parcelable
     * @return
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes to the received Parcel
     * @param parcel
     * @param flags
     */
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

    /**
     * Returns hash code for current object
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Return String form of current object
     */
    @Override
    public String toString() {
        return "CategoryItem: '" + name + '\'';
    }
}
