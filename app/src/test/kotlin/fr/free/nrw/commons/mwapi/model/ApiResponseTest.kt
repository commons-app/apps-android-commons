package fr.free.nrw.commons.mwapi.model

import org.junit.Assert.*
import org.junit.Test

class ApiResponseTest {
    @Test
    fun hasPages_whenQueryIsNull() {
        val response = ApiResponse()
        assertFalse(response.hasPages())
    }

    @Test
    fun hasPages_whenPagesIsNull() {
        val response = ApiResponse()
        response.query = Query()
        response.query.pages = null
        assertFalse(response.hasPages())
    }

    @Test
    fun hasPages_defaultsToSafeValue() {
        val response = ApiResponse()
        response.query = Query()
        assertNotNull(response.query.pages)
        assertTrue(response.hasPages())
    }
}