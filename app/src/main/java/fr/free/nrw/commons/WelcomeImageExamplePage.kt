package fr.free.nrw.commons

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.free.nrw.commons.R

@Composable
fun WelcomeImageExamplePage() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = R.dimen.huge_gap))
            .verticalScroll(rememberScrollState())
            .padding(top = 45.dp, bottom = 50.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.welcome_image_example),
                contentDescription = stringResource(id = R.string.welcome_image_welcome_wikipedia),
                modifier = Modifier
                    .size(if (isLandscape) 200.dp else 300.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.tutorial_4_text),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.standard_gap)))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BulletText(textRes = R.string.tutorial_4_subtext_1)
                BulletText(textRes = R.string.tutorial_4_subtext_2)
                BulletText(textRes = R.string.tutorial_4_subtext_3)
            }
        }
    }
