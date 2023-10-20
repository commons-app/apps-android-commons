package fr.free.nrw.commons

import java.lang.reflect.Field
import java.lang.reflect.Modifier

object TestUtility {
    @Throws(java.lang.Exception::class)
    fun setFinalStatic(field: Field, newValue: Any?) {
        try {
            field.isAccessible = true
            // remove final modifier from field
            val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
            field.set(null, newValue)
        } catch (e: SecurityException) {
            e.stackTrace
        } catch (e: NoSuchFieldException) {
            e.stackTrace
        } catch (e: java.lang.Exception) {
            e.stackTrace
        }
    }
}