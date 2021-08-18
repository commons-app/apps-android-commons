package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Custom Selector View Model test.
 */
class CustomSelectorViewModelTest {

    private lateinit var viewModel: CustomSelectorViewModel

    @Mock
    private lateinit var imageFileLoader: ImageFileLoader

    @Mock
    private lateinit var context: Context

    /**
     * Set up the test.
     */
    @Before
    fun setUp(){
        MockitoAnnotations.initMocks(this)
        viewModel = CustomSelectorViewModel(context, imageFileLoader);
    }

    /**
     * Test onCleared();
     */
    @Test
    fun testOnCleared(){
        val func = viewModel.javaClass.getDeclaredMethod("onCleared")
        func.isAccessible = true
        func.invoke(viewModel);
    }

}