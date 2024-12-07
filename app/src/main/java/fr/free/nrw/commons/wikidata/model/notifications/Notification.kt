package fr.free.nrw.commons.wikidata.model.notifications

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.utils.DateUtil.iso8601DateParse
import fr.free.nrw.commons.wikidata.GsonUtil.defaultGson
import org.apache.commons.lang3.StringUtils
import timber.log.Timber
import java.text.ParseException
import java.util.Date

class Notification {
    private val wiki: String? = null
    private var id: Long = 0
    private val type: String? = null
    private val category: String? = null

    private val title: Title? = null
    private var timestamp: Timestamp? = null

    @SerializedName("*")
    var contents: Contents? = null

    fun wiki(): String = wiki ?: ""

    fun id(): Long = id

    fun setId(id: Long) {
        this.id = id
    }

    fun key(): Long =
        id + wiki().hashCode()

    fun type(): String =
        type ?: ""

    fun title(): Title? = title

    fun getTimestamp(): Date =
        timestamp?.date() ?: Date()

    fun setTimestamp(timestamp: Timestamp?) {
        this.timestamp = timestamp
    }

    val utcIso8601: String
        get() = timestamp?.utciso8601 ?: ""

    val isFromWikidata: Boolean
        get() = wiki() == "wikidatawiki"

    override fun toString(): String =
        id.toString()

    class Title {
        private val full: String? = null
        private val text: String? = null

        fun text(): String = text ?: ""

        fun full(): String = full ?: ""
    }

    class Timestamp {
        internal var utciso8601: String? = null

        fun setUtciso8601(utciso8601: String?) {
            this.utciso8601 = utciso8601
        }

        fun date(): Date {
            try {
                return iso8601DateParse(utciso8601 ?: "")
            } catch (e: ParseException) {
                Timber.e(e)
                return Date()
            }
        }
    }

    class Link {
        var url: String? = null
            get() = field ?: ""
        val label: String? = null
            get() = field ?: ""
        val tooltip: String? = null
            get() = field ?: ""
        private val description: String? = null
        val icon: String? = null
            get() = field ?: ""
    }

    class Links {
        private var primary: JsonElement? = null
        private var primaryLink: Link? = null

        fun setPrimary(primary: JsonElement?) {
            this.primary = primary
        }

        fun getPrimary(): Link? {
            if (primary == null) {
                return null
            }
            if (primaryLink == null && primary is JsonObject) {
                primaryLink = defaultGson.fromJson(primary, Link::class.java)
            }
            return primaryLink
        }
    }

    class Contents {
        val header: String? = null
            get() = field ?: ""
        var compactHeader: String? = null
            get() = field ?: ""
        val body: String? = null
            get() = field ?: ""
        private val icon: String? = null
        var links: Links? = null
    }
}
