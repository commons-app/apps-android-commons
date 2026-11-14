package fr.free.nrw.commons.media

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import android.widget.ImageView
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.MediaType
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.databinding.FragmentMediaDetailBinding
import fr.free.nrw.commons.delete.DeleteHelper
import fr.free.nrw.commons.delete.ReasonBuilder
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.locationpicker.LocationPickerActivity
import fr.free.nrw.commons.ui.widget.HtmlTextView
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import junit.framework.TestCase.assertTrue
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowIntent
import java.lang.reflect.Field
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MediaDetailFragmentUnitTests {
    private val lastLocation = "last_location_while_uploading"
    private lateinit var fragment: MediaDetailFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var context: Context

    @Mock
    private lateinit var deleteHelper: DeleteHelper

    @Mock
    private lateinit var reasonBuilder: ReasonBuilder

    @Mock
    private lateinit var progressBarDeletion: ProgressBar

    @Mock
    private lateinit var delete: Button

    private var isDeleted = true

    @Mock
    private var reasonList: ArrayList<String>? = null

    @Mock
    private var reasonListEnglishMappings: ArrayList<String>? = null

    @Mock
    private lateinit var locationManager: LocationServiceManager

    @Mock
    private lateinit var savedInstanceState: Bundle

    @Mock
    private lateinit var scrollView: ScrollView

    @Mock
    private lateinit var media: Media

    @Mock
    private lateinit var imageView: ImageView

    @Mock
    private lateinit var textView: TextView


    @Mock
    private lateinit var linearLayout: LinearLayout

    @Mock
    private lateinit var button: Button

    @Mock
    private lateinit var detailProvider: MediaDetailProvider

    @Mock
    private lateinit var applicationKvStore: JsonKvStore


    @Mock
    private lateinit var progressBar: ProgressBar



    private lateinit var activity: SearchActivity

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var binding: FragmentMediaDetailBinding

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        context = ApplicationProvider.getApplicationContext()
        OkHttpConnectionFactory.CLIENT = createTestClient()

        activity = Robolectric.buildActivity(SearchActivity::class.java).create().get()

        fragment = MediaDetailFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        layoutInflater = LayoutInflater.from(activity)

        binding = FragmentMediaDetailBinding.inflate(layoutInflater)

        scrollView = binding.mediaDetailScrollView

        progressBarDeletion = binding.progressBarDeletion
        delete = binding.nominateDeletion

        Whitebox.setInternalState(fragment, "media", media)
        Whitebox.setInternalState(fragment, "isDeleted", isDeleted)
        Whitebox.setInternalState(fragment, "reasonList", reasonList)
        Whitebox.setInternalState(fragment, "reasonListEnglishMappings", reasonListEnglishMappings)
        Whitebox.setInternalState(fragment, "reasonBuilder", reasonBuilder)
        Whitebox.setInternalState(fragment, "deleteHelper", deleteHelper)
        Whitebox.setInternalState(fragment, "_binding", binding)
        Whitebox.setInternalState(fragment, "detailProvider", detailProvider)
        Whitebox.setInternalState(binding, "mediaDetailImageView", imageView)
        Whitebox.setInternalState(binding, "mediaDetailTitle", textView)
        Whitebox.setInternalState(binding, "mediaDetailDepictionContainer", linearLayout)
        Whitebox.setInternalState(binding, "dummyCaptionDescriptionContainer", linearLayout)
        Whitebox.setInternalState(binding, "depictionsEditButton", button)
        Whitebox.setInternalState(fragment, "locationManager", locationManager)

        val map = HashMap<String, String>()
        map[Locale.getDefault().language] = ""
        `when`(media.descriptions).thenReturn(map)

        doReturn(mockSharedPreferences).`when`(mockContext).getSharedPreferences(anyString(), anyInt())
        doReturn(mockSharedPreferencesEditor).`when`(mockSharedPreferences).edit()
        doReturn(mockSharedPreferencesEditor).`when`(mockSharedPreferencesEditor).putInt(anyString(), anyInt())
    }

    @Test
    fun `isOwnUpload returns true when uploader matches current user even if author is different`() {
        val accountManager = AccountManager.get(fragment.requireContext())
        shadowOf(accountManager).addAccount(
            Account("Uploader", BuildConfig.ACCOUNT_TYPE),
        )

        `when`(media.user).thenReturn("Uploader")
        `when`(media.author).thenReturn("DifferentAuthor")

        assertTrue(fragment.isOwnUpload())
    }

    @Test
    fun `isOwnUpload returns true when only first character differs in case`() {
        val accountManager = AccountManager.get(fragment.requireContext())
        shadowOf(accountManager).addAccount(
            Account("TestUser", BuildConfig.ACCOUNT_TYPE),
        )

        `when`(media.user).thenReturn("testUser")

        assertTrue(fragment.isOwnUpload())
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateView() {
        Whitebox.setInternalState(fragment, "applicationKvStore", applicationKvStore)
        `when`(applicationKvStore.getBoolean("login_skipped")).thenReturn(true)
        fragment.onCreateView(layoutInflater, null, savedInstanceState)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSaveInstanceState() {
        fragment.onSaveInstanceState(savedInstanceState)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchZoomActivity() {
        `when`(media.imageUrl).thenReturn("")
        fragment.launchZoomActivity(binding.root)
    }

    @Test
    @Throws(Exception::class)
    fun testOnUpdateCoordinatesClickedCurrentLocationNull() {
        `when`(media.coordinates).thenReturn(null)
        `when`(locationManager.getLastLocation()).thenReturn(null)
        `when`(applicationKvStore.getString(lastLocation)).thenReturn("37.773972,-122.431297")
        fragment.onUpdateCoordinatesClicked()
        Mockito.verify(media, Mockito.times(1)).coordinates
        Mockito.verify(locationManager, Mockito.times(1)).getLastLocation()
        val shadowActivity: ShadowActivity = shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shadowIntent: ShadowIntent = shadowOf(startedIntent)
        Assert.assertEquals(shadowIntent.intentClass, LocationPickerActivity::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testOnUpdateCoordinatesClickedNotNullValue() {
        `when`(media.coordinates).thenReturn(LatLng(-0.000001, -0.999999, 0f))
        `when`(applicationKvStore.getString(lastLocation)).thenReturn("37.773972,-122.431297")
        fragment.onUpdateCoordinatesClicked()
        Mockito.verify(media, Mockito.times(3)).coordinates
        val shadowActivity: ShadowActivity = shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shadowIntent: ShadowIntent = shadowOf(startedIntent)
        Assert.assertEquals(shadowIntent.intentClass, LocationPickerActivity::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testOnUpdateCoordinatesClickedCurrentLocationNotNull() {
        `when`(media.coordinates).thenReturn(null)
        `when`(locationManager.getLastLocation()).thenReturn(LatLng(-0.000001, -0.999999, 0f))
        `when`(applicationKvStore.getString(lastLocation)).thenReturn("37.773972,-122.431297")

        fragment.onUpdateCoordinatesClicked()
        Mockito.verify(locationManager, Mockito.times(3)).getLastLocation()
        val shadowActivity: ShadowActivity = shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shadowIntent: ShadowIntent = shadowOf(startedIntent)
        Assert.assertEquals(shadowIntent.intentClass, LocationPickerActivity::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testOnResume() {
        Whitebox.setInternalState(fragment, "applicationKvStore", applicationKvStore)
        `when`(applicationKvStore.getBoolean("login_skipped")).thenReturn(true)
        fragment.onCreateView(layoutInflater, null, savedInstanceState)
        fragment.onResume()
    }

    @Test
    @Throws(Exception::class)
    fun testOnConfigurationChangedCaseTrue() {
        val newConfig = mock(Configuration::class.java)
        fragment.onConfigurationChanged(newConfig)
    }

    @Test
    @Throws(Exception::class)
    fun testOnConfigurationChangedCaseFalse() {
        val newConfig = mock(Configuration::class.java)
        Whitebox.setInternalState(fragment, "heightVerifyingBoolean", false)
        fragment.onConfigurationChanged(newConfig)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroyView() {
        fragment.onDestroyView()
    }

    @Test
    @Throws(Exception::class)
    fun testExtractDescription() {
        fragment.extractDescription("")
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescription() {
        `when`(media.filename).thenReturn("")
        fragment.description
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionList() {
        `when`(media.filename).thenReturn("")
        fragment.descriptionList
    }

    @Test
    @Throws(Exception::class)
    fun testGetCaptions() {
        `when`(media.captions).thenReturn(mapOf(Pair("a", "b")))
        fragment.captions
    }

    @Test
    @Throws(Exception::class)
    fun testGetCaptionsCaseEmpty() {
        `when`(media.captions).thenReturn(mapOf())
        fragment.captions
    }

    @Test
    @Throws(Exception::class)
    fun testSetUpCaptionAndDescriptionLayout() {
        `when`(media.filename).thenReturn("")
        val field: Field =
            MediaDetailFragment::class.java.getDeclaredField("descriptionHtmlCode")
        field.isAccessible = true
        field.set(fragment, null)
        fragment.setUpCaptionAndDescriptionLayout()
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateCategoryDisplayCaseNull() {
        Assert.assertEquals(fragment.updateCategoryDisplay(null), false)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateCategoryDisplayCaseNonNull() {
        Assert.assertEquals(fragment.updateCategoryDisplay(listOf()), true)
    }

    @Test
    @Throws(Exception::class)
    fun testDescriptionEditClicked() {
        `when`(progressBar.visibility).thenReturn(View.VISIBLE)
        `when`(button.visibility).thenReturn(View.GONE)
        `when`(media.filename).thenReturn("")
        fragment.onDescriptionEditClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testShowCaptionAndDescriptionCaseVisible() {
        fragment.showCaptionAndDescription()
    }

    @Test
    @Throws(Exception::class)
    fun testShowCaptionAndDescription() {
        `when`(linearLayout.visibility).thenReturn(View.GONE)
        `when`(media.filename).thenReturn("")
        fragment.showCaptionAndDescription()
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyCoordinatesCaseNull() {
        fragment.prettyCoordinates(media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyCoordinates() {
        `when`(media.coordinates).thenReturn(LatLng(-0.000001, -0.999999, 0f))
        fragment.prettyCoordinates(media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyUploadedDateCaseNull() {
        `when`(media.dateUploaded).thenReturn(null)
        fragment.prettyUploadedDate(media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyUploadedDateCaseNonNull() {
        `when`(media.dateUploaded).thenReturn(Date(2000, 1, 1))
        fragment.prettyUploadedDate(media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyLicenseCaseNull() {
        `when`(media.license).thenReturn(null)
        fragment.prettyLicense(media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyLicenseCaseNonNull() {
        `when`(media.license).thenReturn("licence")
        fragment.prettyLicense(media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyDiscussion() {
        fragment.prettyDiscussion("mock")
    }

    @Test
    @Throws(Exception::class)
    fun testExtractCaptionDescription() {
        fragment.extractCaptionDescription("mock")
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptions() {
        MediaDetailDescriptionParser.parse("mock")
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyCaptionCaseEmpty() {
        `when`(media.captions).thenReturn(mapOf(Pair("a", "")))
        fragment.prettyCaption(media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyCaptionCaseNonEmpty() {
        `when`(media.captions).thenReturn(mapOf(Pair("a", "b")))
        fragment.prettyCaption(media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyCaption() {
        `when`(media.captions).thenReturn(mapOf())
        fragment.prettyCaption(media)
    }

    @Test
    @Throws(Exception::class)
    fun testSetupImageView() {
        fragment.setupImageView(media)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDiscussionLoaded() {
        fragment.onDiscussionLoaded("")
    }

    @Test
    @Throws(Exception::class)
    fun testOnDeleteClickedNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val spinner = mock(Spinner::class.java)
        `when`(media.imageUrl).thenReturn("test@example.com")
        `when`(spinner.selectedItemPosition).thenReturn(0)
        `when`(reasonListEnglishMappings?.get(spinner.selectedItemPosition)).thenReturn("TESTING")
        `when`(
            applicationKvStore.getBoolean(
                String.format(
                    MediaDetailFragment.NOMINATING_FOR_DELETION_MEDIA,
                    media.imageUrl,
                ),
            ),
        ).thenReturn(true)
        doReturn(
            Single.just(true),
        ).`when`(deleteHelper).makeDeletion(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
        )

        doReturn(Single.just("")).`when`(reasonBuilder).getReason(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
        )

        fragment.onDeleteClicked(spinner)
    }

    @Test
    @Throws(Exception::class)
    fun testForMedia() {
        MediaDetailFragment.forMedia(0, true, true, true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDepictEditButtonClicked() {
        fragment.onDepictionsEditButtonClicked()
        verify(linearLayout).removeAllViews()
        verify(button).visibility = GONE
    }

    @Test
    @Throws(Exception::class)
    fun testOnDeleteButtonClicked() {
        fragment.onDeleteButtonClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnCategoryEditButtonClicked() {
        whenever(media.filename).thenReturn("File:Example.jpg")
        fragment.onCategoryEditButtonClicked()
        verify(media, times(1)).filename
    }

    @Test
    @Throws(Exception::class)
    fun testDisplayMediaDetails() {
        whenever(media.filename).thenReturn("File:Example.jpg")
        whenever(media.mediaType).thenReturn(MediaType.IMAGE)
        fragment.displayMediaDetails()
        verify(media, times(4)).filename
    }

    @Test
    @Throws(Exception::class)
    fun testGotoCategoryEditor() {
        fragment.gotoCategoryEditor("[[Category:Test]]")
    }

    @Test
    @Throws(Exception::class)
    fun testOnMediaRefreshed() {
        whenever(media.mediaType).thenReturn(MediaType.IMAGE)
        fragment.onMediaRefreshed(media)
    }

    @Test
    fun testOnImageBackgroundChangedWithDifferentColor() {
        val spyFragment = spy(fragment)
        val color = 0xffffff
        doReturn(mockContext).`when`(spyFragment).context
        doReturn(-1).`when`(mockSharedPreferences).getInt(anyString(), anyInt())

        spyFragment.onImageBackgroundChanged(color)

        verify(imageView, times(1)).setBackgroundColor(color)
        verify(mockSharedPreferencesEditor, times(1)).putInt(anyString(), anyInt())
    }

    @Test
    fun testOnImageBackgroundChangedWithSameColor() {
        val spyFragment = spy(fragment)
        val color = 0
        doReturn(mockContext).`when`(spyFragment).context
        doReturn(color).`when`(mockSharedPreferences).getInt(anyString(), anyInt())

        spyFragment.onImageBackgroundChanged(color)
        verify(imageView, never()).setBackgroundColor(anyInt())
        verify(mockSharedPreferencesEditor, never()).putInt(anyString(), anyInt())
    }

}
