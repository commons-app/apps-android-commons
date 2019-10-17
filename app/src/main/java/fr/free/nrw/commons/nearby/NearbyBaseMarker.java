package fr.free.nrw.commons.nearby;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;

public class NearbyBaseMarker extends BaseMarkerOptions<NearbyMarker, NearbyBaseMarker> {

    public static final Parcelable.Creator<NearbyBaseMarker> CREATOR = new Parcelable.Creator<NearbyBaseMarker>() {
        public NearbyBaseMarker createFromParcel(Parcel in) {
            return new NearbyBaseMarker(in);
        }

        public NearbyBaseMarker[] newArray(int size) {
            return new NearbyBaseMarker[size];
        }
    };

    private Place place;

    public NearbyBaseMarker() {
    }

    private NearbyBaseMarker(Parcel in) {
        position(in.readParcelable(LatLng.class.getClassLoader()));
        snippet(in.readString());
        String iconId = in.readString();
        Bitmap iconBitmap = in.readParcelable(Bitmap.class.getClassLoader());
        Icon icon = IconFactory.recreate(iconId, iconBitmap);
        icon(icon);
        title(in.readString());
        place(in.readParcelable(Place.class.getClassLoader()));
    }

    public NearbyBaseMarker place(Place place) {
        this.place = place;
        return this;
    }

    @Override
    public NearbyBaseMarker getThis() {
        return this;
    }

    @Override
    public NearbyMarker getMarker() {
        return new NearbyMarker(this, place);
    }

    public Place getPlace() {
        return place;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(position, flags);
        dest.writeString(snippet);
        dest.writeString(icon.getId());
        dest.writeParcelable(icon.getBitmap(), flags);
        dest.writeString(title);
        dest.writeParcelable(place, 0);
    }
}
