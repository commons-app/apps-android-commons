package fr.free.nrw.commons.contributions

import android.database.Cursor
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

/**
 * The unit test class for ContributionsPresenter
 */
class ContributionsPresenterTest {
    @Mock
    internal var repository: ContributionsRepository? = null
    @Mock
    internal var view: ContributionsContract.View? = null

    private var contributionsPresenter: ContributionsPresenter? = null

    private lateinit var cursor: Cursor

    lateinit var contribution: Contribution

    lateinit var loader: Loader<Cursor>

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        cursor = Mockito.mock(Cursor::class.java)
        contribution = Mockito.mock(Contribution::class.java)
        contributionsPresenter = ContributionsPresenter(repository)
        loader = Mockito.mock(CursorLoader::class.java)
        contributionsPresenter?.onAttachView(view)
    }


    /**
     * Test presenter actions onGetContributionFromCursor
     */
    @Test
    fun testGetContributionFromCursor() {
        contributionsPresenter?.getContributionsFromCursor(cursor)
        verify(repository)?.getContributionFromCursor(cursor)
    }

    /**
     * Test presenter actions onDeleteContribution
     */
    @Test
    fun testDeleteContribution() {
        contributionsPresenter?.deleteUpload(contribution)
        verify(repository)?.deleteContributionFromDB(contribution)
    }

    /**
     * Test presenter actions on loaderFinished and has non zero media objects
     */
    @Test
    fun testOnLoaderFinishedNonZeroContributions() {
        Mockito.`when`(cursor.count).thenReturn(1)
        contributionsPresenter?.onLoadFinished(loader, cursor)
        verify(view)?.showProgress(false)
        verify(view)?.showWelcomeTip(false)
        verify(view)?.showNoContributionsUI(false)
        verify(view)?.setUploadCount(cursor.count)
    }

    /**
     * Test presenter actions on loaderFinished and has Zero media objects
     */
    @Test
    fun testOnLoaderFinishedZeroContributions() {
        Mockito.`when`(cursor.count).thenReturn(0)
        contributionsPresenter?.onLoadFinished(loader, cursor)
        verify(view)?.showProgress(false)
        verify(view)?.showWelcomeTip(true)
        verify(view)?.showNoContributionsUI(true)
    }


    /**
     * Test presenter actions on loader reset
     */
    @Test
    fun testOnLoaderReset() {
        contributionsPresenter?.onLoaderReset(loader)
        verify(view)?.showProgress(false)
        verify(view)?.showWelcomeTip(true)
        verify(view)?.showNoContributionsUI(true)
    }

    /**
     * Test presenter actions on loader change
     */
    @Test
    fun testOnChanged() {
        contributionsPresenter?.onChanged()
        verify(view)?.onDataSetChanged()
    }


}