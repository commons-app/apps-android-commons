package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;

public class ImageDetails {

     private String name;
     private String title;

    @NonNull public String getName() {
        return name;
    }

    @NonNull public String getTitle() {
        return title;
    }
}
