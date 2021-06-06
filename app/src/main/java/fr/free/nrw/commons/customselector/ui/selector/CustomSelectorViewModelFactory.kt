package fr.free.nrw.commons.customselector.ui.selector

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CustomSelectorViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun<CustomSelectorViewModel: ViewModel?> create(modelClass: Class<CustomSelectorViewModel>):CustomSelectorViewModel {
        return CustomSelectorViewModel(application) as CustomSelectorViewModel
    }

}