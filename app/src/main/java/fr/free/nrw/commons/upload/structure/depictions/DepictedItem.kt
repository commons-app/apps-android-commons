package fr.free.nrw.commons.upload.structure.depictions

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.WikidataItem
import fr.free.nrw.commons.wikidata.WikidataProperties
import fr.free.nrw.commons.wikidata.WikidataProperties.COMMONS_CATEGORY
import fr.free.nrw.commons.wikidata.WikidataProperties.IMAGE
import fr.free.nrw.commons.wikidata.WikidataProperties.INSTANCE_OF
import fr.free.nrw.commons.wikidata.model.DataValue
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.model.StatementPartial
import kotlinx.parcelize.Parcelize
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

const val THUMB_IMAGE_SIZE = "70px"

/**
 * Model class for Depicted Item in Upload and Explore
 */
@Parcelize
@Entity
data class DepictedItem constructor(
    override val name: String,
    val description: String?,
    val imageUrl: String?,
    val instanceOfs: List<String>,
    val commonsCategories: List<CategoryItem>,
    var isSelected: Boolean,
    @PrimaryKey override val id: String,
) : WikidataItem,
    Parcelable {
    constructor(entity: Entities.Entity) : this(
        entity,
        entity.labels().byLanguageOrFirstOrEmpty(),
        entity.descriptions().byLanguageOrFirstOrEmpty(),
    )

    constructor(entity: Entities.Entity, place: Place) : this(
        entity,
        place.name!!,
        place.longDescription!!,
    )

    constructor(entity: Entities.Entity, name: String, description: String) : this(
        name,
        description,
        entity[IMAGE].primaryImageValue?.let {
            getImageUrl(it.value, THUMB_IMAGE_SIZE)
        },
        entity[INSTANCE_OF].toIds(),
        entity[COMMONS_CATEGORY]?.map {
            CategoryItem(
                (it.mainSnak.dataValue as DataValue.ValueString).value,
                "",
                "",
                false,
            )
        }
            ?: emptyList(),
        false,
        entity.id(),
    )

    val primaryImage: String?
        get() = imageUrl?.split('-')?.lastOrNull()

    override fun equals(other: Any?) =
        when {
            this === other -> true
            other is DepictedItem -> name == other.name
            else -> false
        }

    override fun hashCode(): Int = name.hashCode()
}

private fun List<StatementPartial>?.toIds(): List<String> =
    this
        ?.map { it.mainSnak.dataValue }
        ?.filterIsInstance<DataValue.EntityId>()
        ?.map { it.value.id }
        ?: emptyList()

private val List<StatementPartial>?.primaryImageValue: DataValue.ValueString?
    get() = this?.firstOrNull()?.mainSnak?.dataValue as? DataValue.ValueString

operator fun Entities.Entity.get(property: WikidataProperties) = statements?.get(property.propertyName)

private fun Map<String, Entities.Label>.byLanguageOrFirstOrEmpty() =
    let { it[Locale.getDefault().language] ?: it.values.firstOrNull() }?.value() ?: ""

private fun getImageUrl(
    title: String,
    size: String,
): String =
    title
        .substringAfter(":")
        .replace(" ", "_")
        .let {
            val md5Hash = getMd5(it)
            "https://upload.wikimedia.org/wikipedia/commons/thumb/${md5Hash[0]}/${md5Hash[0]}${md5Hash[1]}/$it/$size-$it"
        }

/**
 * Generates MD5 hash for the filename
 */
private fun getMd5(input: String): String =
    try {
        // Static getInstance method is called with hashing MD5
        val md = MessageDigest.getInstance("MD5")

        // digest() method is called to calculate message digest
        //  of an input digest() return array of byte
        val messageDigest = md.digest(input.toByteArray())

        // Convert byte array into signum representation
        val no = BigInteger(1, messageDigest)

        // Convert message digest into hex value
        var hashtext = no.toString(16)
        while (hashtext.length < 32) {
            hashtext = "0$hashtext"
        }
        hashtext
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException(e)
    }
