package fr.free.nrw.commons.delete

import android.content.Context
import android.content.res.Resources
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.achievements.FeedbackResponse
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.utils.ViewUtilWrapper
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.*


class ReasonBuilderTest {

    @Mock
    internal var sessionManager: SessionManager? = null
    @Mock
    internal var okHttpJsonApiClient: OkHttpJsonApiClient? = null
    @Mock
    internal var context: Context? = null
    @Mock
    internal var viewUtilWrapper: ViewUtilWrapper? = null

    @InjectMocks
    var reasonBuilder: ReasonBuilder? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val resources = mock(Resources::class.java)
        `when`(resources!!.getString(anyInt())).thenReturn("test")
        `when`(context!!.resources).thenReturn(resources)
    }

    @Test
    fun forceLoginWhenAccountIsNull() {
        reasonBuilder!!.getReason(mock(Media::class.java), "test")
        verify(sessionManager, times(1))!!.forceLogin(any(Context::class.java))
    }

    @Test
    fun getReason() {
        `when`(sessionManager?.userName).thenReturn("Testuser")
        `when`(sessionManager?.doesAccountExist()).thenReturn(true)
        `when`(okHttpJsonApiClient!!.getAchievements(anyString()))
                .thenReturn(Single.just(mock(FeedbackResponse::class.java)))

        val media = mock(Media::class.java)
        `when`(media!!.dateUploaded).thenReturn(Date())

        reasonBuilder!!.getReason(media, "test")
        verify(sessionManager, times(0))!!.forceLogin(any(Context::class.java))
        verify(okHttpJsonApiClient, times(1))!!.getAchievements(anyString())
    }
}