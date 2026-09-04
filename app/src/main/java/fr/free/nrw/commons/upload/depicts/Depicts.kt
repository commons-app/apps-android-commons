package fr.free.nrw.commons.upload.depicts

import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import java.util.Date

/**
 *  entity class for DepictsRoomDateBase
 */
data class Depicts(
    val item: DepictedItem,
    val lastUsed: Date,
)
