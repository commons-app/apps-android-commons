package fr.free.nrw.commons.LocationPicker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.CameraPosition
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

/**
 * Observes live camera position data
 */
class LocationPickerViewModel(
    application: Application
): AndroidViewModel(application), Callback<CameraPosition> {

    /**
     * Wrapping CameraPosition with MutableLiveData
     */
    val result = MutableLiveData<CameraPosition?>()

    /**
     * Responses on camera position changing
     *
     * @param call     Call<CameraPosition>
     * @param response Response<CameraPosition>
     */
    override fun onResponse(
        call: Call<CameraPosition>,
        response: Response<CameraPosition>
    ) {
        if(response.body() == null) {
            result.value = null
            return
        }
        result.value = response.body()
    }

    override fun onFailure(call: Call<CameraPosition>, t: Throwable) {
        Timber.e(t)
    }
}