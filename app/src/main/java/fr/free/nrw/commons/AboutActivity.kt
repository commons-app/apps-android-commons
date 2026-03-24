package fr.free.nrw.commons

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.ui.widget.HtmlTextView
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.handleWebUrl
import java.util.*

// grouped the actions into a single class for the betteer scalability
data class AboutActions(
    val onBackClick: () -> Unit,
    val onShareClick: () -> Unit,
    val onLaunchFacebook: () -> Unit,
    val onLaunchGithub: () -> Unit,
    val onLaunchWebsite: () -> Unit,
    val onRateUs: () -> Unit,
    val onUserGuide: () -> Unit,
    val onPrivacyPolicy: () -> Unit,
    val onTranslate: () -> Unit,
    val onCredits: () -> Unit,
    val onFaq: () -> Unit
)

class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CommonsTheme {
                val actions = AboutActions(
                    onBackClick = { finish() },
                    onShareClick = { shareApp() },
                    onLaunchFacebook = { launchFacebook() },
                    onLaunchGithub = { launchGithub() },
                    onLaunchWebsite = { launchWebsite() },
                    onRateUs = { launchRatings() },
                    onUserGuide = { launchUserGuide() },
                    onPrivacyPolicy = { launchPrivacyPolicy() },
                    onTranslate = { launchTranslate() },
                    onCredits = { launchCredits() },
                    onFaq = { launchFrequentlyAskedQuestions() }
                )

                AboutScreen(
                    version = applicationContext.getVersionNameWithSha(),
                    actions = actions
                )
            }
        }
    }

    // logic methods remain within the Activity to keep theUI pure
    @SuppressLint("StringFormatInvalid")
    private fun shareApp() {
        val shareText = String.format(getString(R.string.share_text), Urls.PLAY_STORE_URL_PREFIX + packageName)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)))
    }

    private fun launchFacebook() {
        try {
            startActivity(Intent(ACTION_VIEW, Urls.FACEBOOK_APP_URL.toUri()).setPackage(Urls.FACEBOOK_PACKAGE_NAME))
        } catch (e: Exception) {
            handleWebUrl(this, Urls.FACEBOOK_WEB_URL.toUri())
        }
    }

    private fun launchGithub() {
        try {
            startActivity(Intent(ACTION_VIEW, Urls.GITHUB_REPO_URL.toUri()).setPackage(Urls.GITHUB_PACKAGE_NAME))
        } catch (e: Exception) {
            handleWebUrl(this, Urls.GITHUB_REPO_URL.toUri())
        }
    }

    private fun launchWebsite() = handleWebUrl(this, Urls.WEBSITE_URL.toUri())
    private fun launchCredits() = handleWebUrl(this, Urls.CREDITS_URL.toUri())
    private fun launchUserGuide() = handleWebUrl(this, Urls.USER_GUIDE_URL.toUri())
    private fun launchPrivacyPolicy() = handleWebUrl(this, BuildConfig.PRIVACY_POLICY_URL.toUri())
    private fun launchFrequentlyAskedQuestions() = handleWebUrl(this, Urls.FAQ_URL.toUri())

    private fun launchRatings() {
        try {
            startActivity(Intent(ACTION_VIEW, (Urls.PLAY_STORE_PREFIX + packageName).toUri()))
        } catch (_: ActivityNotFoundException) {
            handleWebUrl(this, (Urls.PLAY_STORE_URL_PREFIX + packageName).toUri())
        }
    }

    private fun launchTranslate() {
        val instance = CommonsApplication.instance
        val sortedNames = instance.languageLookUpTable!!.getCanonicalNames().toMutableList()
        Collections.sort(sortedNames)
        val spinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@AboutActivity, android.R.layout.simple_spinner_dropdown_item, sortedNames)
        }
        showAlertDialog(this, getString(R.string.about_translate_title), getString(R.string.about_translate_message),
            getString(R.string.about_translate_proceed), getString(R.string.about_translate_cancel),
            {
                val langCode = instance.languageLookUpTable!!.getCodes()[spinner.selectedItemPosition]
                handleWebUrl(this, (Urls.TRANSLATE_WIKI_URL + langCode).toUri())
            }, {}, spinner
        )
    }
}
//custom theme wrapper to centralize the color logic
@Composable
fun CommonsTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val background = if (isDarkTheme) colorResource(R.color.main_background_dark) else colorResource(R.color.main_background_light)

    val colorScheme = if (isDarkTheme) {
        darkColorScheme(primary = colorResource(R.color.primaryColor), background = background, onBackground = Color.White)
    } else {
        lightColorScheme(primary = colorResource(R.color.primaryColor), background = background, onBackground = Color.Black)
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    version: String,
    actions: AboutActions
) {
    val logoAlpha = remember { Animatable(0f) }
    // smoooth fade in for the app logo on opening the screen
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(1000))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.about), fontWeight = FontWeight.Bold, maxLines = 1,
                        softWrap = false, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = actions.onShareClick) {
                        Icon(Icons.Default.Share, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.alpha(logoAlpha.value)) {
                Image(painter = painterResource(R.drawable.ic_launcher), contentDescription = stringResource(R.string.commons_logo),
                    modifier = Modifier.size(110.dp).clip(CircleShape))
            }

            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            // tucked the version info inside a small pill shaped chip to look better
            AssistChip(
                onClick = { },
                label = { Text(version, style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            HtmlText(stringResource(R.string.about_license))
            HtmlText(String.format(stringResource(R.string.about_improve), Urls.NEW_ISSUE_URL), Modifier.padding(top = 12.dp))

            // grouped the social media icons in a neat row
            Row(modifier = Modifier.padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                EnhancedSocialIcon(R.drawable.ic_action_website, actions.onLaunchWebsite)
                EnhancedSocialIcon(R.drawable.ic_action_facebook, actions.onLaunchFacebook)
                EnhancedSocialIcon(R.drawable.ic_action_github, actions.onLaunchGithub)
            }

            // grouping the all link items into a single card layout
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    EnhancedLinkRow(stringResource(R.string.about_rate_us), actions.onRateUs)
                    DividerRow()
                    EnhancedLinkRow(stringResource(R.string.user_guide), actions.onUserGuide)
                    DividerRow()
                    EnhancedLinkRow(stringResource(R.string.about_privacy_policy), actions.onPrivacyPolicy)
                    DividerRow()
                    EnhancedLinkRow(stringResource(R.string.about_translate), actions.onTranslate)
                    DividerRow()
                    EnhancedLinkRow(stringResource(R.string.about_credits), actions.onCredits)
                    DividerRow()
                    EnhancedLinkRow(stringResource(R.string.about_faq), actions.onFaq)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EnhancedSocialIcon(drawableId: Int, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    Surface(modifier = Modifier.size(52.dp).clickable { onClick() }, shape = CircleShape, color = tint.copy(alpha = 0.1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painter = painterResource(drawableId), contentDescription = null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun EnhancedLinkRow(text: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun DividerRow() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val textColor = if (isSystemInDarkTheme()) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    // bridge thhe existing xml-based HtmlTextView into the compose ui
    AndroidView(
        modifier = modifier,
        factory = { context -> HtmlTextView(context).apply { gravity = android.view.Gravity.CENTER } },
        update = { view ->
            view.setTextColor(textColor)
            view.setHtmlText(html)
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    CommonsTheme {
        AboutScreen(
            version = "v6.4.0-debug-master",
            actions = AboutActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        )
    }
}