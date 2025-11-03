package fr.free.nrw.commons.nearby

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label.Companion.fromText
import fr.free.nrw.commons.nearby.model.NearbyResultItem
import fr.free.nrw.commons.utils.LocationUtils.calculateDistance
import fr.free.nrw.commons.utils.PlaceUtils.latLngFromPointString
import org.apache.commons.lang3.StringUtils
import timber.log.Timber

/**
 * A single geolocated Wikidata item
 */
@Entity(tableName = "place")
class Place : Parcelable {
    var language: String?
    var name: String?
    var label: Label?
    var longDescription: String?
    var category: String?
    var pic: String?
    var distance: String? = null
    var siteLinks: Sitelinks?
    var isMonument: Boolean = false
    var thumb: String? = null

    @Embedded
    var location: LatLng?

    @PrimaryKey
    var entityID: String = "dummy"

    // exists boolean will tell whether the place exists or not,
    // For a place to be existing both destroyed and endTime property should be null but it is also not necessary for a non-existing place to have both properties either one property is enough (in such case that not given property will be considered as null).
    var exists: Boolean?

    constructor() {
        language = null
        name = null
        label = null
        longDescription = null
        location = null
        category = null
        pic = null
        exists = null
        siteLinks = null
        entityID = "dummy"
    }

    constructor(
        language: String?,
        name: String?,
        label: Label?,
        longDescription: String?,
        location: LatLng?,
        category: String?,
        siteLinks: Sitelinks?,
        pic: String?,
        exists: Boolean?,
        entityID: String
    ) {
        this.language = language
        this.name = name
        this.label = label
        this.longDescription = longDescription
        this.location = location
        this.category = category
        this.siteLinks = siteLinks
        this.pic = pic ?: ""
        this.exists = exists
        this.entityID = entityID
    }

    constructor(
        language: String?,
        name: String?,
        label: Label?,
        longDescription: String?,
        location: LatLng?,
        category: String?,
        siteLinks: Sitelinks?,
        pic: String?,
        exists: Boolean?
    ) {
        this.language = language
        this.name = name
        this.label = label
        this.longDescription = longDescription
        this.location = location
        this.category = category
        this.siteLinks = siteLinks
        this.pic = pic ?: ""
        this.exists = exists
    }

    constructor(
        name: String?, longDescription: String?, location: LatLng?, category: String?,
        siteLinks: Sitelinks?, pic: String?, thumb: String?, entityID: String
    ) {
        this.name = name
        this.longDescription = longDescription
        this.location = location
        this.category = category
        this.siteLinks = siteLinks
        this.pic = pic ?: ""
        this.thumb = thumb
        language = null
        label = null
        exists = true
        this.entityID = entityID
    }

    constructor(`in`: Parcel) {
        language = `in`.readString()
        name = `in`.readString()
        label = `in`.readSerializable() as Label?
        longDescription = `in`.readString()
        location = `in`.readParcelable<LatLng?>(LatLng::class.java.classLoader)
        category = `in`.readString()
        siteLinks = `in`.readParcelable<Sitelinks?>(Sitelinks::class.java.classLoader)
        val picString = `in`.readString()
        pic = picString ?: ""
        val existString: String = `in`.readString()!!
        exists = existString.toBoolean()
        isMonument = `in`.readInt() == 1
        entityID = `in`.readString()!!
    }

    /**
     * Gets the distance between place and curLatLng
     */
    fun getDistanceInDouble(curLatLng: LatLng): Double {
        return calculateDistance(
            curLatLng.latitude, curLatLng.longitude,
            this.location!!.latitude, this.location!!.longitude
        )
    }


    val wikiDataEntityId: String?
        /**
         * Extracts the entity id from the wikidata link
         *
         * @return returns the entity id if wikidata link destroyed
         */
        get() {
            if (!entityID.isEmpty()) {
                return entityID
            }

            if (!hasWikidataLink()) {
                Timber.d(
                    "Wikidata entity ID is null for place with sitelink %s",
                    siteLinks.toString()
                )
                return null
            }

            //Determine entityID from link
            val wikiDataLink = siteLinks!!.wikidataUri.toString()

            if (wikiDataLink.contains("http://www.wikidata.org/entity/")) {
                entityID = wikiDataLink.substring("http://www.wikidata.org/entity/".length)
                return entityID
            }
            return null
        }

    /**
     * Checks if the Wikidata item has a Wikipedia page associated with it
     *
     * @return true if there is a Wikipedia link
     */
    fun hasWikipediaLink(): Boolean {
        return !siteLinks?.wikipediaUri?.toString().isNullOrEmpty()
    }

    /**
     * Checks if the Wikidata item has a Wikidata page associated with it
     *
     * @return true if there is a Wikidata link
     */
    fun hasWikidataLink(): Boolean {
        return !siteLinks?.wikidataUri?.toString().isNullOrEmpty()
    }

    /**
     * Checks if the Wikidata item has a Commons page associated with it
     *
     * @return true if there is a Commons link
     */
    fun hasCommonsLink(): Boolean {
        return !siteLinks?.commonsUri?.toString().isNullOrEmpty()
    }

    /**
     * Check if we already have the exact same Place
     *
     * @param other Place being tested
     * @return true if name and location of Place is exactly the same
     */
    override fun equals(other: Any?): Boolean {
        if (other is Place) {
            val that = other
            return name == that.name && location == that.location
        } else {
            return false
        }
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + location.hashCode()
    }

    override fun toString(): String {
        return "Place{" +
                "name='" + name + '\'' +
                ", lang='" + language + '\'' +
                ", label='" + label + '\'' +
                ", longDescription='" + longDescription + '\'' +
                ", location='" + location + '\'' +
                ", category='" + category + '\'' +
                ", distance='" + distance + '\'' +
                ", siteLinks='" + siteLinks.toString() + '\'' +
                ", pic='" + pic + '\'' +
                ", exists='" + exists.toString() + '\'' +
                ", entityID='" + entityID + '\'' +
                '}'
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(language)
        dest.writeString(name)
        dest.writeSerializable(label)
        dest.writeString(longDescription)
        dest.writeParcelable(location, 0)
        dest.writeString(category)
        dest.writeParcelable(siteLinks, 0)
        dest.writeString(pic)
        dest.writeString(entityID)
        dest.writeString(exists.toString())
        dest.writeInt(if (isMonument) 1 else 0)
    }

    companion object {
        fun from(item: NearbyResultItem): Place {
            val itemClass = item.getClassName().value
            var classEntityId = ""
            if (!StringUtils.isBlank(itemClass)) {
                classEntityId = itemClass.replace("http://www.wikidata.org/entity/", "")
            }
            var entityId = ""
            if (!StringUtils.isBlank(item.getItem().value)) {
                entityId = item.getItem().value.replace("http://www.wikidata.org/entity/", "")
            }
            // Set description when not null and not empty
            var description =
                if (item.getDescription().value != null && !item.getDescription().value
                        .isEmpty()
                ) item.getDescription().value else ""
            // When description is "?" but we have a valid label, just use the label. So replace "?" by "" in description
            description = (if (description == "?"
                && (item.getLabel().value != null
                        && !item.getLabel().value.isEmpty())
            ) "" else description)
            /*
         * If we have a valid label
         *     - If have a valid label add the description at the end of the string with parenthesis
         *     - If we don't have a valid label, string will include only the description. So add it without paranthesis
         */
            description = (if (item.getLabel().value != null && !item.getLabel().value.isEmpty())
                (item.getLabel().value
                        + (if (description != null && !description.isEmpty()) " (" + description + ")" else ""))
            else
                description)
            return Place(
                item.getLabel().language,
                item.getLabel().value,
                fromText(classEntityId),  // list
                description,  // description and label of Wikidata item
                latLngFromPointString(item.getLocation().value),
                item.getCommonsCategory().value,
                Sitelinks(
                    wikipediaLink = item.getWikipediaArticle().value,
                    commonsLink = item.getCommonsArticle().value,
                    wikidataLink = item.getItem().value
                ),
                item.getPic().value,  // Checking if the place exists or not
                (item.getDestroyed().value === "") && (item.getEndTime().value === "")
                        && (item.getDateOfOfficialClosure().value === "")
                        && (item.getPointInTime().value === ""),
                entityId
            )
        }

        @JvmField
        val CREATOR: Parcelable.Creator<Place?> = object : Parcelable.Creator<Place?> {
            override fun createFromParcel(`in`: Parcel): Place {
                return Place(`in`)
            }

            override fun newArray(size: Int): Array<Place?> {
                return arrayOfNulls(size)
            }
        }
    }
}
