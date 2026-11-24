package fr.free.nrw.commons.contributions

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.Media
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkInfo
import java.util.Date

/**
 * Automatic retries of failed uploads that must wait, because the server asked to or the last
 * tries failed with a server problem.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = Application::class)
class ContributionsFragmentRetryWaitTest {
    private lateinit var fragment: ContributionsFragment
    private lateinit var presenter: ContributionsPresenter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network =
            ShadowNetworkInfo.newInstance(
                NetworkInfo.DetailedState.CONNECTED,
                ConnectivityManager.TYPE_WIFI,
                0,
                true,
                true,
            )
        shadowOf(connectivity).setActiveNetworkInfo(network)
        fragment = spy(ContributionsFragment())
        doReturn(context).`when`(fragment).context
        presenter = mock(ContributionsPresenter::class.java)
        fragment.contributionsPresenter = presenter
    }

    @Test
    fun failedUploadIsNotRetriedBeforeItsWaitEnds() {
        val contribution = failed(Contribution.STATE_FAILED, retryNotBefore = inTwoMinutes())

        fragment.retryUpload(contribution)

        assertEquals(Contribution.STATE_FAILED, contribution.state)
        assertEquals(0, contribution.retries)
        verify(presenter, never()).checkDuplicateImageAndRestartContribution(contribution)
    }

    @Test
    fun captionsAndDepictsAreNotRetriedBeforeTheirWaitEnds() {
        val contribution = failed(Contribution.STATE_METADATA_PENDING, retryNotBefore = inTwoMinutes())

        fragment.retryUpload(contribution)

        assertEquals(Contribution.STATE_METADATA_PENDING, contribution.state)
        assertEquals(0, contribution.retries)
        verify(presenter, never()).saveContribution(contribution)
    }

    @Test
    fun failedUploadIsRetriedOnceItsWaitEnded() {
        val contribution = failed(Contribution.STATE_FAILED, retryNotBefore = Date(System.currentTimeMillis() - 1))

        fragment.retryUpload(contribution)

        assertEquals(1, contribution.retries)
        verify(presenter).checkDuplicateImageAndRestartContribution(contribution)
    }

    @Test
    fun failedUploadWithoutWaitIsRetriedRightAway() {
        val contribution = failed(Contribution.STATE_FAILED, retryNotBefore = null)

        fragment.retryUpload(contribution)

        assertEquals(1, contribution.retries)
        verify(presenter).checkDuplicateImageAndRestartContribution(contribution)
    }

    private fun inTwoMinutes() = Date(System.currentTimeMillis() + 120_000)

    private fun failed(
        state: Int,
        retryNotBefore: Date?,
    ) = Contribution(
        media = Media(),
        state = state,
        errorInfo = "Unsuccessful response: HTTP 503",
        retryNotBefore = retryNotBefore,
    )
}
