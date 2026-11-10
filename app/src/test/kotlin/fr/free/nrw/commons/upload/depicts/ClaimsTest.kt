package fr.free.nrw.commons.upload.depicts

import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.mwapi.MwException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClaimsTest {
    @Test
    fun claimsAreRead() {
        val claims = GsonUtil.defaultGson.fromJson(CLAIMS, Claims::class.java)

        assertEquals(listOf(CLAIM_ID), claims.claims["P180"]?.map { it.id })
    }

    @Test
    fun plaintextErrorsThrowMwException() {
        // Without this, the error would be read as a file without depictions
        val exception =
            assertThrows(MwException::class.java) {
                GsonUtil.defaultGson.fromJson(PLAINTEXT_ERRORS, Claims::class.java)
            }

        assertEquals("param-invalid", exception.errorCode)
    }

    private companion object {
        const val CLAIM_ID = "M181291919\$C1BDEBD1-451A-4531-95DB-277B34616504"

        // Answers from Commons to action=wbgetclaims&property=P180 with errorformat=plaintext
        const val CLAIMS =
            """{"claims":{"P180":[{"mainsnak":{"snaktype":"value","property":"P180",""" +
                """"hash":"5f5252beb135a5907bda03f66580df69bdf5d691","datavalue":{"value":{"entity-type":"item",""" +
                """"numeric-id":769934,"id":"Q769934"},"type":"wikibase-entityid"},"datatype":"wikibase-item"},""" +
                """"type":"statement","id":"$CLAIM_ID","rank":"normal"}]}}"""
        const val PLAINTEXT_ERRORS =
            """{"errors":[{"code":"param-invalid","text":"The serialization \"M999999999999\" is not """ +
                """recognized by the configured id builders","module":"wbgetclaims"}]}"""
    }
}
