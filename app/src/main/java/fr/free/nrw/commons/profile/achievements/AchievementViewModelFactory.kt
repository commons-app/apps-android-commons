package fr.free.nrw.commons.profile.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

/**
 * This class extends the ViewModelProvider.Factory and creates a ViewModelFactory class
 * for AchievementViewModel
 */
class AchievementViewModelFactory @Inject constructor(
    private val viewModelProvider: Provider<AchievementViewModel>
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AchievementViewModel::class.java)) {
            (@Suppress("UNCHECKED_CAST")
            return viewModelProvider.get() as T)
        } else {
            throw IllegalArgumentException("Unknown class name")
        }
    }
}