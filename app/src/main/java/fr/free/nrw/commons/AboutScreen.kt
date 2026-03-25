package fr.free.nrw.commons

import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import fr.free.nrw.commons.CommonsApplication.Companion.instance
import java.util.Collections
import androidx.compose.ui.graphics.toArgb
import android.text.method.LinkMovementMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onNavigateUp: () -> Unit,
    onShareClicked: () -> Unit,
    onFacebookClicked: () -> Unit,
    onGithubClicked: () -> Unit,
    onWebsiteClicked: () -> Unit,
    onRateUsClicked: () -> Unit,
    onUserGuideClicked: () -> Unit,
    onPrivacyPolicyClicked: () -> Unit,
    onTranslateProceed: (String) -> Unit,
    onCreditsClicked: () -> Unit,
    onFaqClicked: () -> Unit
) {
    var showTranslateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.title_activity_about)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShareClicked) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = stringResource(id = R.string.commons_logo)
            )

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = versionName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            HtmlText(
                htmlString = stringResource(id = R.string.about_license),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            val improveText = stringResource(id = R.string.about_improve).format(Urls.NEW_ISSUE_URL)
            HtmlText(
                htmlString = improveText,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onWebsiteClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_website),
                        contentDescription = stringResource(id = R.string.commons_website)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onFacebookClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_facebook),
                        contentDescription = stringResource(id = R.string.commons_facebook)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onGithubClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_github),
                        contentDescription = stringResource(id = R.string.commons_github)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LinkText(text = stringResource(id = R.string.about_rate_us), onClick = onRateUsClicked)
            LinkText(text = stringResource(id = R.string.user_guide), onClick = onUserGuideClicked)
            LinkText(text = stringResource(id = R.string.about_privacy_policy), onClick = onPrivacyPolicyClicked)
            LinkText(text = stringResource(id = R.string.about_translate), onClick = { showTranslateDialog = true })
            LinkText(text = stringResource(id = R.string.about_credits), onClick = onCreditsClicked)
            LinkText(text = stringResource(id = R.string.about_faq), onClick = onFaqClicked)

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showTranslateDialog) {
            TranslateDialog(
                onDismiss = { showTranslateDialog = false },
                onProceed = { langCode ->
                    onTranslateProceed(langCode)
                    showTranslateDialog = false
                }
            )
        }
    }
}

@Composable
fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    )
}

@Composable
fun HtmlText(htmlString: String, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                movementMethod = LinkMovementMethod.getInstance()

            }
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(htmlString, HtmlCompat.FROM_HTML_MODE_COMPACT)
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateDialog(onDismiss: () -> Unit, onProceed: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val sortedLocalizedNamesRef = instance.languageLookUpTable?.getCanonicalNames() ?: emptyList()
    val languageCodes = instance.languageLookUpTable?.getCodes() ?: emptyList()

    Collections.sort(sortedLocalizedNamesRef)

    var selectedIndex by remember { mutableStateOf(0) }
    val selectedLanguage = if (sortedLocalizedNamesRef.isNotEmpty()) sortedLocalizedNamesRef[selectedIndex] else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.about_translate_title)) },
        text = {
            Column {
                Text(text = stringResource(R.string.about_translate_message))
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedLanguage,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        sortedLocalizedNamesRef.forEachIndexed { index, langName ->
                            DropdownMenuItem(
                                text = { Text(langName) },
                                onClick = {
                                    selectedIndex = index
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (languageCodes.isNotEmpty()) {
                    onProceed(languageCodes[selectedIndex])
                }
            }) {
                Text(stringResource(R.string.about_translate_proceed))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.about_translate_cancel))
            }
        }
    )
}