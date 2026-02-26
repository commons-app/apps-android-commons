package fr.free.nrw.commons.customselector.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.customselector.domain.ImageRepository
import fr.free.nrw.commons.customselector.domain.use_case.ImageUseCase
import fr.free.nrw.commons.customselector.ui.screens.CustomSelectorViewModel
import javax.inject.Inject

class CustomSelectorViewModelFactory @Inject constructor(
    private val imageRepository: ImageRepository,
    private val imageUseCase: ImageUseCase
): ViewModelProvider.Factory {
    override fun <CustomSelectorViewModel : ViewModel> create(
        modelClass: Class<CustomSelectorViewModel>
    ): CustomSelectorViewModel {
        return CustomSelectorViewModel(
            imageRepository, imageUseCase
        ) as CustomSelectorViewModel
    }
}