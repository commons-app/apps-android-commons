package fr.free.nrw.commons.nearby.model

import com.google.gson.annotations.SerializedName

class NearbyResultItem(
    private val item: ResultTuple?,
    private val wikipediaArticle: ResultTuple?,
    private val commonsArticle: ResultTuple?,
    private val location: ResultTuple?,
    @field:SerializedName("itemLabel")
    private val label: ResultTuple?,
    @field:SerializedName("streetAddress") private val address: ResultTuple?,
    private val icon: ResultTuple?,
    @field:SerializedName("class") private val className: ResultTuple?,
    @field:SerializedName("classLabel") private val classLabel: ResultTuple?,
    @field:SerializedName("commonsCategory") private val commonsCategory: ResultTuple?,
    @field:SerializedName("pic") private val pic: ResultTuple?,
    @field:SerializedName("destroyed") private val destroyed: ResultTuple?,
    @field:SerializedName("itemDescription") private val description: ResultTuple?,
    @field:SerializedName("endTime") private val endTime: ResultTuple?,
    @field:SerializedName("monument") private val monument: ResultTuple?,
    @field:SerializedName("dateOfOfficialClosure") private val dateOfOfficialClosure: ResultTuple?,
    @field:SerializedName("pointInTime") private val pointInTime: ResultTuple?,
) {
    fun getItem(): ResultTuple = item ?: ResultTuple()

    fun getWikipediaArticle(): ResultTuple = wikipediaArticle ?: ResultTuple()

    fun getCommonsArticle(): ResultTuple = commonsArticle ?: ResultTuple()

    fun getLocation(): ResultTuple = location ?: ResultTuple()

    fun getLabel(): ResultTuple = label ?: ResultTuple()

    fun getIcon(): ResultTuple = icon ?: ResultTuple()

    fun getClassName(): ResultTuple = className ?: ResultTuple()

    fun getClassLabel(): ResultTuple = classLabel ?: ResultTuple()

    fun getCommonsCategory(): ResultTuple = commonsCategory ?: ResultTuple()

    fun getPic(): ResultTuple = pic ?: ResultTuple()

    fun getDestroyed(): ResultTuple = destroyed ?: ResultTuple()

    fun getDateOfOfficialClosure(): ResultTuple = dateOfOfficialClosure ?: ResultTuple()

    fun getDescription(): ResultTuple = description ?: ResultTuple()

    fun getEndTime(): ResultTuple = endTime ?: ResultTuple()

    fun getAddress(): String = address?.value ?: ""

    fun getMonument(): ResultTuple? = monument

    fun getPointInTime(): ResultTuple = pointInTime ?: ResultTuple()

}
