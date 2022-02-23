package fr.free.nrw.commons.explore;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;

public class ExplorePlace extends Place {

    public String name;
    public String longDescription;
    public LatLng location;
    public String pic;
    public String commonsURL;
    public String distance;
    public String thumb;

    public ExplorePlace(final String name, final String longDescription,
        final LatLng location, final String pic, final String commonsURL, final String thumb) {
        this.name = name;
        this.longDescription = longDescription;
        this.location = location;
        this.pic = pic;
        this.thumb = thumb;
    }

    @Override
    public String toString() {
        return "ExplorePlace{" +
            "name='" + name + '\'' +
            ", longDescription='" + longDescription + '\'' +
            ", location=" + location +
            ", pic='" + pic + '\'' +
            ", commonsURL='" + commonsURL + '\'' +
            ", distance='" + distance + '\'' +
            ", thumb='" + thumb + '\'' +
            '}';
    }
}
