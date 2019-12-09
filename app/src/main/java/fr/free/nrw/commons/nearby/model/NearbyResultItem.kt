package fr.free.nrw.commons.nearby.model

import com.google.gson.annotations.SerializedName

class NearbyResultItem(private val item: ResultTuple?,
                       private val wikipediaArticle: ResultTuple?,
                       private val commonsArticle: ResultTuple?,
                       private val location: ResultTuple?,
                       private val label: ResultTuple?,
                       private val icon: ResultTuple?, @field:SerializedName("class") private val className: ResultTuple?,
                       @field:SerializedName("classLabel") private val classLabel: ResultTuple?,
                       @field:SerializedName("commonsCategory") private val commonsCategory: ResultTuple?,
                       @field:SerializedName("pic") private val pic: ResultTuple?,
                       @field:SerializedName("destroyed") private val destroyed: ResultTuple?) {

    fun getItem(): ResultTuple {
        return item ?: ResultTuple()
    }

    fun getWikipediaArticle(): ResultTuple {
        return wikipediaArticle ?: ResultTuple()
    }

    fun getCommonsArticle(): ResultTuple {
        return commonsArticle ?: ResultTuple()
    }

    fun getLocation(): ResultTuple {
        return location ?: ResultTuple()
    }

    fun getLabel(): ResultTuple {
        return label ?: ResultTuple()
    }

    fun getIcon(): ResultTuple {
        return icon ?: ResultTuple()
    }

    fun getClassName(): ResultTuple {
        return className ?: ResultTuple()
    }

    fun getClassLabel(): ResultTuple {
        return classLabel ?: ResultTuple()
    }

    fun getCommonsCategory(): ResultTuple {
        return commonsCategory ?: ResultTuple()
    }

    fun getPic(): ResultTuple {
        return pic ?: ResultTuple()
    }

    fun getDestroyed(): ResultTuple {
        return destroyed ?: ResultTuple()
    }

}