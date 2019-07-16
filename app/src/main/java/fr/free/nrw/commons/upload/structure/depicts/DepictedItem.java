package fr.free.nrw.commons.upload.structure.depicts;

import android.widget.ImageView;

public class DepictedItem {
    private final String depictsLabel;
    private final String description;
    private final ImageView imageView;
    private boolean selected;
    private String entityId;

    public DepictedItem(String depictsLabel, String description, ImageView imageView, boolean selected, String entityId) {
        this.depictsLabel = depictsLabel;
        this.selected = selected;
        this.description = description;
        this.imageView = imageView;
        this.entityId = entityId;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDescription() {
        return description;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public String getDepictsLabel() {
        return depictsLabel;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
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

        return depictsLabel.equals(that.depictsLabel);
    }
}
