package fr.free.nrw.commons.locationpicker

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.LocationPicker.LocationPickerViewModel
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import retrofit2.Call
import retrofit2.Response

class LocationPickerViewModelUnitTests {

    private lateinit var viewModel: LocationPickerViewModel

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var call: Call<CameraPosition>

    @Mock
    private lateinit var response: Response<CameraPosition>

    @Mock
    private lateinit var result: MutableLiveData<CameraPosition>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        viewModel = LocationPickerViewModel(application)

        Whitebox.setInternalState(viewModel, "result", result)
    }

    @Test
    fun `Test onResponse when response body is null`() {
        viewModel.onResponse(call, response)
        verify(call, times(0)).isExecuted
        verify(response, times(1)).body()
        verify(result, times(1)).value = null
    }

    @Test
    fun `Test onResponse when response body is not null`() {
        whenever(response.body()).thenReturn(mock(CameraPosition::class.java))
        viewModel.onResponse(call, response)
        verify(call, times(0)).isExecuted
        verify(response, times(2)).body()
        verify(result, times(1)).value = any()
    }

    @Test
    fun testOnFailure() {
        viewModel.onFailure(call, mock(Throwable::class.java))
        verify(call, times(0)).isExecuted
    }

    @Test
    fun testGetResult() {
        viewModel.result
    }

}