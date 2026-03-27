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
import androidx.compose.foundation.lazy.LazyColumn
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
import fr.free.nrw.commons.theme.*
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

data class AboutLinkItem(val textRes: Int, val onClick: () -> Unit)
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
    internal fun shareApp() {
        val shareText = String.format(getString(R.string.share_text), Urls.PLAY_STORE_URL_PREFIX + packageName)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)))
    }

    internal fun launchFacebook() {
        try {
            startActivity(Intent(ACTION_VIEW, Urls.FACEBOOK_APP_URL.toUri()).setPackage(Urls.FACEBOOK_PACKAGE_NAME))
        } catch (e: Exception) {
            handleWebUrl(this, Urls.FACEBOOK_WEB_URL.toUri())
        }
    }

    internal fun launchGithub() {
        try {
            startActivity(Intent(ACTION_VIEW, Urls.GITHUB_REPO_URL.toUri()).setPackage(Urls.GITHUB_PACKAGE_NAME))
        } catch (e: Exception) {
            handleWebUrl(this, Urls.GITHUB_REPO_URL.toUri())
        }
    }

    internal fun launchWebsite() = handleWebUrl(this, Urls.WEBSITE_URL.toUri())
    internal fun launchCredits() = handleWebUrl(this, Urls.CREDITS_URL.toUri())
    internal fun launchUserGuide() = handleWebUrl(this, Urls.USER_GUIDE_URL.toUri())
    internal fun launchPrivacyPolicy() = handleWebUrl(this, BuildConfig.PRIVACY_POLICY_URL.toUri())
    internal fun launchFrequentlyAskedQuestions() = handleWebUrl(this, Urls.FAQ_URL.toUri())

    internal fun launchRatings() {
        try {
            startActivity(Intent(ACTION_VIEW, (Urls.PLAY_STORE_PREFIX + packageName).toUri()))
        } catch (_: ActivityNotFoundException) {
            handleWebUrl(this, (Urls.PLAY_STORE_URL_PREFIX + packageName).toUri())
        }
    }

    internal fun launchTranslate() {
        val instance = CommonsApplication.instance
        val sortedNames = instance.languageLookUpTable!!.getCanonicalNames().toMutableList().apply { Collections.sort(this) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(version: String, actions: AboutActions) {
    val logoAlpha = remember { Animatable(Spacing.none.value) }
    LaunchedEffect(Unit) { logoAlpha.animateTo(1f, animationSpec = tween(1000)) }

    val aboutLinks = remember {
        listOf(
            AboutLinkItem(R.string.about_rate_us, actions.onRateUs),
            AboutLinkItem(R.string.user_guide, actions.onUserGuide),
            AboutLinkItem(R.string.about_privacy_policy, actions.onPrivacyPolicy),
            AboutLinkItem(R.string.about_translate, actions.onTranslate),
            AboutLinkItem(R.string.about_credits, actions.onCredits),
            AboutLinkItem(R.string.about_faq, actions.onFaq)
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    CommonsText(text = stringResource(R.string.about), preset = CommonsTextPreset.Title, color = TextWhite, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = actions.onShareClick) {
                        Icon(Icons.Default.Share, null, tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = Spacing.large, vertical = Spacing.medium)
        ) {
            item {
                Box(modifier = Modifier.alpha(logoAlpha.value)) {
                    Image(painter = painterResource(R.drawable.ic_launcher),
                        contentDescription = stringResource(R.string.commons_logo),
                        modifier = Modifier.size(Dimensions.logoSize).clip(CircleShape))
                }
                CommonsText(text = stringResource(R.string.app_name),
                    preset = CommonsTextPreset.Headline, fontWeight = FontWeight.Bold)
                AssistChip(
                    onClick = { },
                    label = { CommonsText(text = version, preset = CommonsTextPreset.Caption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                    modifier = Modifier.padding(top = Spacing.extraSmall, bottom = Spacing.medium)
                )
                HtmlText(stringResource(R.string.about_license))
                HtmlText(String.format(stringResource(R.string.about_improve), Urls.NEW_ISSUE_URL),
                    Modifier.padding(top = Spacing.medium))
                Row(modifier = Modifier.padding(vertical = Spacing.extraLarge),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.huge)) {
                    EnhancedSocialIcon(R.drawable.ic_action_website, actions.onLaunchWebsite)
                    EnhancedSocialIcon(R.drawable.ic_action_facebook, actions.onLaunchFacebook)
                    EnhancedSocialIcon(R.drawable.ic_action_github, actions.onLaunchGithub)
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.cardCorner),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.cardElevation)
                ) {
                    Column {
                        aboutLinks.forEachIndexed { index, item ->
                            EnhancedLinkRow(stringResource(item.textRes), item.onClick)
                            if (index < aboutLinks.size - 1) DividerRow()
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(Spacing.huge)) }
        }
    }
}

@Composable
fun EnhancedSocialIcon(drawableId: Int, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    Surface(modifier = Modifier.size(Dimensions.socialIconBox).clickable { onClick() },
        shape = CircleShape, color = tint.copy(alpha = 0.1f)) {
        Box(contentAlignment = Alignment.Center) {
            Image(painter = painterResource(drawableId), contentDescription = null,
                colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(Dimensions.socialIcon))
        }
    }
}

@Composable
fun EnhancedLinkRow(text: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        CommonsText(text = text, preset = CommonsTextPreset.Body)
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(Dimensions.linkIcon))
    }
}

@Composable
fun DividerRow() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.medium),
        thickness = Dimensions.dividerThickness, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onBackground
    AndroidView(
        modifier = modifier,
        factory = { context -> HtmlTextView(context).apply { gravity = android.view.Gravity.CENTER } },
        update = { view ->
            view.setTextColor(textColor.hashCode())
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