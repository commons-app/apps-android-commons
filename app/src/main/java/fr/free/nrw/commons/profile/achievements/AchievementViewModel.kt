package fr.free.nrw.commons.profile.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.free.nrw.commons.profile.model.UserAchievements
import fr.free.nrw.commons.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AchievementViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _achievements = MutableStateFlow(UserAchievements(
        LevelController.LevelInfo.LEVEL_1,
        articlesUsingImagesCount = 0,
        thanksReceivedCount = 0,
        featuredImagesCount = 0,
        qualityImagesCount = 0,
        imagesUploadedCount = 0,
        revertedCount = 0,
        uniqueImagesCount = 0,
        imagesEditedBySomeoneElseCount = 0
        )
    )
    val achievements : StateFlow<UserAchievements> = _achievements

    private val _loading = MutableStateFlow(true)
    val loading : StateFlow<Boolean> = _loading

    fun getUserAchievements(username: String){
        viewModelScope.launch {
            repository.getUserLevel(username = username).collect {
                _loading.value = false
                _achievements.value = it
            }
        }
    }
}