package fr.free.nrw.commons.db

import depictedItem
import fr.free.nrw.commons.contributions.Contribution
import media
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Checks that a queued upload keeps the place where its picture was taken once it is stored in
 * the database, separately from its depictions.
 */
class ContributionLocationOfCreationDaoTest : InMemoryDatabaseTest() {
    private fun queued(
        pageId: String,
        location: String?,
        depictions: List<String>,
    ) = Contribution(
        media = media(pageId = pageId, filename = "File:$pageId.jpg"),
        state = Contribution.STATE_QUEUED,
        depictedItems = depictions.map { depictedItem(id = it, name = it) },
        locationOfCreation = location?.let { depictedItem(id = it, name = "Detroit") },
    )

    @Test
    fun `queued upload keeps its shooting location and depictions`() {
        val dao = roomDatabase.contributionDao()
        dao.saveSynchronous(queued("1", location = "Q12439", depictions = listOf("Q_ENGINE")))

        val stored = dao.getContribution("1")

        assertEquals("Q12439", stored.locationOfCreation?.id)
        assertEquals("Detroit", stored.locationOfCreation?.name)
        assertEquals(listOf("Q_ENGINE"), stored.depictedItems.map { it.id })
    }

    @Test
    fun `same item can be stored as both depiction and shooting location`() {
        val dao = roomDatabase.contributionDao()
        dao.saveSynchronous(queued("2", location = "Q12439", depictions = listOf("Q12439")))

        val stored = dao.getContribution("2")

        assertEquals("Q12439", stored.locationOfCreation?.id)
        assertEquals(listOf("Q12439"), stored.depictedItems.map { it.id })
    }

    @Test
    fun `upload without a shooting location is stored with a null location`() {
        val dao = roomDatabase.contributionDao()
        dao.saveSynchronous(queued("3", location = null, depictions = listOf("Q_ENGINE")))

        assertNull(dao.getContribution("3").locationOfCreation)
    }
}
