package fr.free.nrw.commons.LocationPicker;

import com.mapbox.mapboxsdk.maps.Style;

/**
 * Constants need for location picking
 */
public final class LocationPickerConstants {

    public static final String ACTIVITY_KEY
        = "location.picker.activity";

    public static final String MAP_CAMERA_POSITION
        = "location.picker.cameraPosition";

    public static final String DARK_MAP_STYLE
        = Style.getPredefinedStyle("Dark");

    public static final String STREETS_MAP_STYLE
        = Style.getPredefinedStyle("Streets");


    private LocationPickerConstants() {
    }
}
