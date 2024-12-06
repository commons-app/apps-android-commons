package fr.free.nrw.commons.wikidata.json

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import fr.free.nrw.commons.wikidata.json.annotations.Required
import java.io.IOException
import java.lang.reflect.Field

/**
 * TypeAdapterFactory that provides TypeAdapters that return null values for objects that are
 * missing fields annotated with @Required.
 *
 * BEWARE: This means that a List or other Collection of objects that have @Required fields can
 * contain null elements after deserialization!
 *
 * TODO: Handle null values in lists during deserialization, perhaps with a new @RequiredElements
 * annotation and another corresponding TypeAdapter(Factory).
 */
class RequiredFieldsCheckOnReadTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        val rawType: Class<*> = typeToken.rawType
        val requiredFields = collectRequiredFields(rawType)

        if (requiredFields.isEmpty()) {
            return null
        }

        for (field in requiredFields) {
            field.isAccessible = true
        }

        return Adapter(gson.getDelegateAdapter(this, typeToken), requiredFields)
    }

    private fun collectRequiredFields(clazz: Class<*>): Set<Field> = buildSet {
        for (field in clazz.declaredFields) {
            if (field.isAnnotationPresent(Required::class.java)) add(field)
        }
    }

    private class Adapter<T>(
        private val delegate: TypeAdapter<T>,
        private val requiredFields: Set<Field>
    ) : TypeAdapter<T>() {

        @Throws(IOException::class)
        override fun write(out: JsonWriter, value: T?) =
            delegate.write(out, value)

        @Throws(IOException::class)
        override fun read(reader: JsonReader): T? =
            if (allRequiredFieldsPresent(delegate.read(reader), requiredFields))
                delegate.read(reader)
            else
                null

        fun allRequiredFieldsPresent(deserialized: T, required: Set<Field>): Boolean {
            for (field in required) {
                try {
                    if (field[deserialized] == null) return false
                } catch (e: IllegalArgumentException) {
                    throw JsonParseException(e)
                } catch (e: IllegalAccessException) {
                    throw JsonParseException(e)
                }
            }
            return true
        }
    }
}
