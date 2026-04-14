package fr.free.nrw.commons.nearby

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.free.nrw.commons.location.LatLng

/**
 * Room entity for the place table.
 */
@Entity(tableName = "place")
data class PlaceRoomEntity(
    val language: String?,
    val name: String?,
    val label: Label?,
    val longDescription: String?,
    @Embedded
    val location: LatLng?,
    @PrimaryKey
    val entityID: String,
    val category: String?,
    val pic: String?,
    val exists: Boolean?,
    val distance: String?,
    val siteLinks: Sitelinks?,
    val isMonument: Boolean,
    val thumb: String?
)
