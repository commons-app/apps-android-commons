package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;

public class ImageDetails {

    @SuppressWarnings("unused") private String name;
    @SuppressWarnings("unused") private String title;

    @NonNull public String getName() {
        return name;
    }

    @NonNull public String getTitle() {
        return title;
    }
}
