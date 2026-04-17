package fr.free.nrw.commons

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import fr.free.nrw.commons.utils.handleWebUrl

@Composable
fun WelcomeFinalPage(onFinishClicked: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(id = R.dimen.huge_gap)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Row(
                modifier = Modifier.height(180.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.welcome_wikipedia),
                    contentDescription = stringResource(id = R.string.welcome_image_welcome_wikipedia),
                    modifier = Modifier.size(width = 150.dp, height = 180.dp),
                    contentScale = ContentScale.Fit
                )

                Image(
                    painter = painterResource(id = R.drawable.welcome_copyright),
                    contentDescription = stringResource(id = R.string.welcome_image_welcome_copyright),
                    modifier = Modifier
                        .height(120.dp)
                        .width(dimensionResource(id = R.dimen.giant_height))
                        .padding(start = dimensionResource(id = R.dimen.standard_gap)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.standard_gap)))

            Text(
                text = stringResource(id = R.string.welcome_final_text),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.widthIn(max = dimensionResource(id = R.dimen.very_large_height))
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.standard_gap)))

            Button(
                onClick = onFinishClicked,
                colors = ButtonDefaults.buttonColors(
                    contentColor = colorResource(id = R.color.primaryColor)
                ),
                modifier = Modifier
                    .width(120.dp)
                    .height(dimensionResource(id = R.dimen.overflow_button_dimen))
            ) {
                Text(
                    text = stringResource(id = R.string.welcome_final_button_text),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = stringResource(id = R.string.welcome_help_button_text),
            color = Color.White,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = dimensionResource(id = R.dimen.standard_gap),
                    bottom = dimensionResource(id = R.dimen.standard_gap)
                )
                .clickable {
                    handleWebUrl(context, Uri.parse("https://commons.wikimedia.org/wiki/Help:Contents"))
                }
                .padding(dimensionResource(id = R.dimen.standard_gap)) // Inner padding to make it easier to tap
        )
    }
}