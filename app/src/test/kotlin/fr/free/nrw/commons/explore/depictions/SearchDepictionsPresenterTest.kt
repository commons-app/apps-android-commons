package fr.free.nrw.commons.explore.depictions

import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SearchDepictionsFragmentPresenterTest {

    @Mock
    internal lateinit var view: SearchDepictionsFragmentContract.View

    private lateinit var searchDepictionsFragmentPresenter: SearchDepictionsFragmentPresenter

    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var searchableDepictionsDataSourceFactory: SearchableDepictionsDataSourceFactory

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        searchDepictionsFragmentPresenter = SearchDepictionsFragmentPresenter(
            testScheduler,
            searchableDepictionsDataSourceFactory
        )
        searchDepictionsFragmentPresenter.onAttachView(view)
    }

}
