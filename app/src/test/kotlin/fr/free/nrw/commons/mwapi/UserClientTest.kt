package fr.free.nrw.commons.mwapi

import fr.free.nrw.commons.utils.DateUtil
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import fr.free.nrw.commons.wikidata.mwapi.UserInfo
import io.reactivex.Observable
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.Date

class UserClientTest {
    @Mock
    internal var userInterface: UserInterface? = null

    @InjectMocks
    var userClient: UserClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun isUserBlockedFromCommonsForInfinitelyBlockedUser() {
        val userInfo = Mockito.mock(UserInfo::class.java)
        Mockito.`when`(userInfo.blockexpiry()).thenReturn("infinite")
        val mwQueryResult = Mockito.mock(MwQueryResult::class.java)
        Mockito.`when`(mwQueryResult.userInfo()).thenReturn(userInfo)
        val mockResponse = Mockito.mock(MwQueryResponse::class.java)
        Mockito.`when`(mockResponse.query()).thenReturn(mwQueryResult)
        Mockito
            .`when`(userInterface!!.getUserBlockInfo())
            .thenReturn(Observable.just(mockResponse))

        val isBanned = userClient!!.isUserBlockedFromCommons().blockingGet()
        assertTrue(isBanned)
    }

    @Test
    fun isUserBlockedFromCommonsForTimeBlockedUser() {
        val currentDate = Date()
        val expiredDate = Date(currentDate.time + 10000)

        val userInfo = Mockito.mock(UserInfo::class.java)
        Mockito.`when`(userInfo.blockexpiry()).thenReturn(DateUtil.iso8601DateFormat(expiredDate))
        val mwQueryResult = Mockito.mock(MwQueryResult::class.java)
        Mockito.`when`(mwQueryResult.userInfo()).thenReturn(userInfo)
        val mockResponse = Mockito.mock(MwQueryResponse::class.java)
        Mockito.`when`(mockResponse.query()).thenReturn(mwQueryResult)
        Mockito
            .`when`(userInterface!!.getUserBlockInfo())
            .thenReturn(Observable.just(mockResponse))

        val isBanned = userClient!!.isUserBlockedFromCommons().blockingGet()
        assertTrue(isBanned)
    }

    @Test
    fun isUserBlockedFromCommonsForNeverBlockedUser() {
        val userInfo = Mockito.mock(UserInfo::class.java)
        Mockito.`when`(userInfo.blockexpiry()).thenReturn("")
        val mwQueryResult = Mockito.mock(MwQueryResult::class.java)
        Mockito.`when`(mwQueryResult.userInfo()).thenReturn(userInfo)
        val mockResponse = Mockito.mock(MwQueryResponse::class.java)
        Mockito.`when`(mockResponse.query()).thenReturn(mwQueryResult)
        Mockito
            .`when`(userInterface!!.getUserBlockInfo())
            .thenReturn(Observable.just(mockResponse))

        val isBanned = userClient!!.isUserBlockedFromCommons().blockingGet()
        assertFalse(isBanned)
    }
}
