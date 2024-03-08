package fr.free.nrw.commons.mwapi

import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.*
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import fr.free.nrw.commons.wikidata.mwapi.UserInfo
import fr.free.nrw.commons.utils.DateUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.`is`
import java.util.*

class UserClientTest{
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
        Mockito.`when`(userInterface!!.getUserBlockInfo())
                .thenReturn(Observable.just(mockResponse))

        val isBanned = userClient!!.isUserBlockedFromCommons().blockingGet()
        assertThat(isBanned, `is`(true))
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
        Mockito.`when`(userInterface!!.getUserBlockInfo())
                .thenReturn(Observable.just(mockResponse))

        val isBanned = userClient!!.isUserBlockedFromCommons().blockingGet()
        assertThat(isBanned, `is`(true))
    }

    @Test
    fun isUserBlockedFromCommonsForNeverBlockedUser() {
        val userInfo = Mockito.mock(UserInfo::class.java)
        Mockito.`when`(userInfo.blockexpiry()).thenReturn("")
        val mwQueryResult = Mockito.mock(MwQueryResult::class.java)
        Mockito.`when`(mwQueryResult.userInfo()).thenReturn(userInfo)
        val mockResponse = Mockito.mock(MwQueryResponse::class.java)
        Mockito.`when`(mockResponse.query()).thenReturn(mwQueryResult)
        Mockito.`when`(userInterface!!.getUserBlockInfo())
                .thenReturn(Observable.just(mockResponse))

        val isBanned = userClient!!.isUserBlockedFromCommons().blockingGet()
        assertThat(isBanned, `is`(false))
    }

}