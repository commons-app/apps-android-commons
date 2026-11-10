package fr.free.nrw.commons.upload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.Contribution
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class FailedUploadsFragmentTest {
    private lateinit var context: Context
    private lateinit var presenter: PendingUploadsPresenter
    private lateinit var fragment: FailedUploadsFragment

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        presenter = mock()
        fragment = spy(FailedUploadsFragment())
        doReturn(context).whenever(fragment).context
        fragment.pendingUploadsPresenter = presenter
    }

    @Test
    fun `retrying one upload shows a message and restarts it`() {
        val contribution = mock<Contribution>()

        fragment.restartUpload(contribution)

        assertEquals(context.getString(R.string.retrying_upload), ShadowToast.getTextOfLatestToast())
        verify(presenter).restartUpload(contribution, context)
    }

    @Test
    fun `retrying all uploads shows a message and restarts them`() {
        fragment.restartUploads()

        assertEquals(context.getString(R.string.retrying_failed_uploads), ShadowToast.getTextOfLatestToast())
        verify(presenter).restartFailedUploads(any())
    }
}
