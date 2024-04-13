package fr.free.nrw.commons.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.free.nrw.commons.auth.login.LoginCallback
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.di.CommonsApplicationComponent
import kotlinx.coroutines.launch
import javax.inject.Inject

class LoginViewModel : ViewModel() {
    @Inject
    lateinit var loginClient: LoginClient

    fun doLogin(
        username: String,
        password: String,
        twoFactorCode: String,
        userLanguage: String,
        loginCallback: LoginCallback
    ) = viewModelScope.launch {
        loginClient.doLogin(username, password, twoFactorCode, userLanguage, loginCallback)
    }
}

// TODO: there are ways to do a generic Dagger viewmodel factory, but these days Google's
//       "Hilt" handles all that, so until we migrate to Hilt, create a factory per view
//       model and look to delete them after the migration.
@Suppress("UNCHECKED_CAST")
class LoginViewModelFactory(private val component: CommonsApplicationComponent) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val loginViewModel = LoginViewModel()

        component.inject(loginViewModel)

        return loginViewModel as T
    }
}
