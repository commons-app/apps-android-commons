package fr.free.nrw.commons.wikidata.mwapi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MwQueryPageTest {
    private val didymUsage = MwQueryPage.FileUsage().apply {
        setTitle("User:Didym/Mobile upload")
    }

    @Test
    fun checkWhetherFileIsUsedInWikis_nullGlobalUsages() {
        assertFalse(checkWhetherFileIsUsedInWikis(null, null))
    }

    @Test
    fun checkWhetherFileIsUsedInWikis_emptyGlobalUsages() {
        assertFalse(checkWhetherFileIsUsedInWikis(emptyList(), null))
    }

    @Test
    fun checkWhetherFileIsUsedInWikis_emptyFileUsage() {
        assertFalse(checkWhetherFileIsUsedInWikis(emptyList(), emptyList()))
    }

    @Test
    fun checkWhetherFileIsUsedInWikis_singleGlobalUsages() {
        assertTrue(checkWhetherFileIsUsedInWikis(listOf(MwQueryPage.GlobalUsage()), null))
    }

    @Test
    fun checkWhetherFileIsUsedInWikis_singleFileUsageContainsDidym() {
        assertFalse(checkWhetherFileIsUsedInWikis(null, listOf(didymUsage)))
    }

    @Test
    fun checkWhetherFileIsUsedInWikis_didymIgnoredInList() {
        assertTrue(
            checkWhetherFileIsUsedInWikis(
                null, listOf(
                    didymUsage, MwQueryPage.FileUsage().apply { setTitle("somewhere else") }
                )
            )
        )
    }

}