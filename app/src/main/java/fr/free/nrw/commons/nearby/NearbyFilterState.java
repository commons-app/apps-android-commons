package fr.free.nrw.commons.nearby;

public class NearbyFilterState {
    private boolean existsSelected;
    private boolean needPhotoSelected;

    private static NearbyFilterState nearbyFılterStateInstance;

    /**
     * Define initial filter values here
     */
    private NearbyFilterState() {
        existsSelected = false;
        needPhotoSelected = true;
    }

    public static NearbyFilterState getInstance() {
        if (nearbyFılterStateInstance == null) {
            nearbyFılterStateInstance = new NearbyFilterState();
        }
        return nearbyFılterStateInstance;
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

}
