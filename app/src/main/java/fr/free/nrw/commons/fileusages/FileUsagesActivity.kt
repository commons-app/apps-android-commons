package fr.free.nrw.commons.fileusages

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.theme.BaseActivity
import javax.inject.Inject

class FileUsagesActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: FileUsagesViewModelProviderFactory

    private val viewModel: FileUsagesViewModel by viewModels<FileUsagesViewModel> {
        viewModelFactory
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme(
                    primary = colorResource(R.color.primaryDarkColor),
                    surface = colorResource(R.color.main_background_dark),
                    background = colorResource(R.color.main_background_dark)
                ) else lightColorScheme(
                    primary = colorResource(R.color.primaryColor),
                    surface = colorResource(R.color.main_background_light),
                    background = colorResource(R.color.main_background_light)
                )
            ) {
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                    TopAppBar(title = { Text(text = "File usages") }, navigationIcon = {
                        IconButton(onClick = {
                            onBackPressedDispatcher.onBackPressed()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = null
                            )
                        }
                    })
                }) { innerPadding ->
                    FileUsagesScreen(modifier = Modifier.padding(innerPadding), viewModel)
                }
            }
        }
    }
}

