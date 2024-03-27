package fr.free.nrw.commons.LocationPicker;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import fr.free.nrw.commons.CameraPosition;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Observes live camera position data
 */
public class LocationPickerViewModel extends AndroidViewModel implements Callback<CameraPosition> {

    /**
     * Wrapping CameraPosition with MutableLiveData
     */
    private final MutableLiveData<CameraPosition> result = new MutableLiveData<>();

    /**
     * Constructor for this class
     *
     * @param application Application
     */
    public LocationPickerViewModel(@NonNull final Application application) {
        super(application);
    }

    /**
     * Responses on camera position changing
     *
     * @param call     Call<CameraPosition>
     * @param response Response<CameraPosition>
     */
    @Override
    public void onResponse(final @NotNull Call<CameraPosition> call,
        final Response<CameraPosition> response) {
        if (response.body() == null) {
            result.setValue(null);
            return;
        }
        result.setValue(response.body());
    }

    @Override
    public void onFailure(final @NotNull Call<CameraPosition> call, final @NotNull Throwable t) {
        Timber.e(t);
    }

    /**
     * Gets live CameraPosition
     *
     * @return MutableLiveData<CameraPosition>
     */
    public MutableLiveData<CameraPosition> getResult() {
        return result;
    }

}
