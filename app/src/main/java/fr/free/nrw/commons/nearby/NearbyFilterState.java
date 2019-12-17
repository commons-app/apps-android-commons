package fr.free.nrw.commons.nearby;

import java.util.ArrayList;

public class NearbyFilterState {
    private boolean existsSelected;
    private boolean needPhotoSelected;
    private int checkBoxTriState;
    private ArrayList<Label> selectedLabels;

    private static NearbyFilterState nearbyFılterStateInstance;

    /**
     * Define initial filter values here
     */
    private NearbyFilterState() {
        existsSelected = false;
        needPhotoSelected = true;
        checkBoxTriState = -1; // Unknown
        selectedLabels = new ArrayList<>(); // Initially empty
    }

    public static NearbyFilterState getInstance() {
        if (nearbyFılterStateInstance == null) {
            nearbyFılterStateInstance = new NearbyFilterState();
        }
        return nearbyFılterStateInstance;
    }

    public static void setSelectedLabels(ArrayList<Label> selectedLabels) {
        getInstance().selectedLabels = selectedLabels;
    }

    public static void setExistsSelected(boolean existsSelected) {
        getInstance().existsSelected = existsSelected;
    }

    public static void setNeedPhotoSelected(boolean needPhotoSelected) {
        getInstance().needPhotoSelected = needPhotoSelected;
    }

    public boolean isExistsSelected() {
        return existsSelected;
    }

    public boolean isNeedPhotoSelected() {
        return needPhotoSelected;
    }

    public int getCheckBoxTriState() {
        return checkBoxTriState;
    }

    public ArrayList<Label> getSelectedLabels() {
        return selectedLabels;
    }
}
