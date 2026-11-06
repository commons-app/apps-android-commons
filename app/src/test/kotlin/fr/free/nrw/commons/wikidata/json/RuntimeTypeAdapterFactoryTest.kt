package fr.free.nrw.commons.wikidata.json

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Test class for [RuntimeTypeAdapterFactory]
 */
class RuntimeTypeAdapterFactoryTest {
    @Test
    fun testNullBaseTypeIsRejectedWithMessage() {
        val exception =
            assertThrows(NullPointerException::class.java) {
                RuntimeTypeAdapterFactory<Any>(null, "type", false)
            }

        assertEquals("baseType and typeFieldName must not be null", exception.message)
    }

    @Test
    fun testNullTypeFieldNameIsRejectedWithMessage() {
        val exception =
            assertThrows(NullPointerException::class.java) {
                RuntimeTypeAdapterFactory<Any>(Any::class.java, null, false)
            }

        assertEquals("baseType and typeFieldName must not be null", exception.message)
    }

    @Test
    fun testRegisterSubtypeWithNullLabelIsRejectedWithMessage() {
        val factory = RuntimeTypeAdapterFactory<Any>(Any::class.java, "type", false)

        val exception =
            assertThrows(NullPointerException::class.java) {
                factory.registerSubtype(String::class.java, null)
            }

        assertEquals("type and label must not be null", exception.message)
    }
}
