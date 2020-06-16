package fr.free.nrw.commons.category;

/**
 * Callback for notifying the viewpager that the number of items have changed and for requesting
 * more images when the viewpager has been scrolled to its end.
 */

public interface CategoryImagesCallback {

  void viewPagerNotifyDataSetChanged();

  void requestMoreImages();
}


