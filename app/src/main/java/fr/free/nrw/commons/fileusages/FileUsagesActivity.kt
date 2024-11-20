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
    lateinit var viewModelFactory: FileUsagesViewModelProviderFactory

    private val viewModel: FileUsagesViewModel by viewModels<FileUsagesViewModel> {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val media = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("media", Media::class.java)
        } else {
            intent.getParcelableExtra("media")
        }
        viewModel.setFileName(media?.filename)
//        viewModel.getOtherWikisUsage()
        println(media?.filename)
        setContent {
//TODO also need theming
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FileUsagesScreen(modifier = Modifier.padding(innerPadding), viewModel)
                }
            }
        }
    }
}

