package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject

/**
 * View Model Factory.
 */
class CustomSelectorViewModelFactory @Inject constructor(val context: Context,val imageFileLoader: ImageFileLoader) : ViewModelProvider.Factory {

    override fun<CustomSelectorViewModel: ViewModel> create(modelClass: Class<CustomSelectorViewModel>) : CustomSelectorViewModel {
        return CustomSelectorViewModel(context,imageFileLoader) as CustomSelectorViewModel
    }

}