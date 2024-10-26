package fr.free.nrw.commons.fileusages

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.theme.BaseActivity
import javax.inject.Inject

class FileUsagesActivity : BaseActivity() {


    @Inject
    lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    private val viewModel: FileUsagesViewModel by viewModels(factoryProducer = {
        FileUsagesViewModelProviderFactory(
            okHttpJsonApiClient
        )
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val media = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("media", Media::class.java)
        } else {
            intent.getParcelableExtra("media")
        }
        viewModel.setMedia(media)
        viewModel.getCommonsFileUsages()
        viewModel.getOtherWikisUsages()
        setContent {
//TODO also need theming
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FileUsagesScreen(modifier = Modifier.padding(innerPadding), viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //TODO[Parry] see if we should keep it in composable screen
        viewModel.disposeNetworkOperations()
    }
}

