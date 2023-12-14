package fr.free.nrw.commons.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewTreeObserver
import android.webkit.WebView
import android.widget.*
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.soloader.SoLoader
import com.nhaarman.mockitokotlin2.whenever
import com.nhaarman.mockitokotlin2.doReturn
import fr.free.nrw.commons.LocationPicker.LocationPickerActivity
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.delete.DeleteHelper
import fr.free.nrw.commons.delete.ReasonBuilder
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.ui.widget.HtmlTextView
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.*
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowIntent
import org.wikipedia.AppAdapter
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MediaDetailFragmentUnitTests {

    private val REQUEST_CODE = 1001
    private val LAST_LOCATION = "last_location_while_uploading"
    private lateinit var fragment: MediaDetailFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var view: View
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
    private lateinit var simpleDraweeView: SimpleDraweeView

    @Mock
    private lateinit var textView: TextView

    @Mock
    private lateinit var htmlTextView: HtmlTextView

    @Mock
    private lateinit var linearLayout: LinearLayout

    @Mock
    private lateinit var genericDraweeHierarchy: GenericDraweeHierarchy

    @Mock
    private lateinit var button: Button

    @Mock
    private lateinit var detailProvider: MediaDetailPagerFragment.MediaDetailProvider

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    @Mock
    private lateinit var webView: WebView

    @Mock
    private lateinit var progressBar: ProgressBar

    @Mock
    private lateinit var listView: ListView

    @Mock
    private lateinit var intent: Intent

    private lateinit var activity: SearchActivity
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockSharedPreferencesEditor:  SharedPreferences.Editor

    @Before
    fun setUp() {

        MockitoAnnotations.openMocks(this)

        context = ApplicationProvider.getApplicationContext()
        AppAdapter.set(TestAppAdapter())

        SoLoader.setInTestMode()

        Fresco.initialize(ApplicationProvider.getApplicationContext())

        activity = Robolectric.buildActivity(SearchActivity::class.java).create().get()

        fragment = MediaDetailFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        layoutInflater = LayoutInflater.from(activity)

        view = LayoutInflater.from(activity)
            .inflate(R.layout.fragment_media_detail, null) as View

        scrollView = view.findViewById(R.id.mediaDetailScrollView)
        Whitebox.setInternalState(fragment, "scrollView", scrollView)

        progressBarDeletion = view.findViewById(R.id.progressBarDeletion)
        delete = view.findViewById(R.id.nominateDeletion)

        Whitebox.setInternalState(fragment, "media", media)
        Whitebox.setInternalState(fragment, "isDeleted", isDeleted)
        Whitebox.setInternalState(fragment, "reasonList", reasonList)
        Whitebox.setInternalState(fragment, "reasonListEnglishMappings", reasonListEnglishMappings)
        Whitebox.setInternalState(fragment, "reasonBuilder", reasonBuilder)
        Whitebox.setInternalState(fragment, "deleteHelper", deleteHelper)
        Whitebox.setInternalState(fragment, "progressBar", progressBar)
        Whitebox.setInternalState(fragment, "progressBarEditDescription", progressBar)
        Whitebox.setInternalState(fragment, "captionsListView", listView)
        Whitebox.setInternalState(fragment, "descriptionWebView", webView)
        Whitebox.setInternalState(fragment, "detailProvider", detailProvider)
        Whitebox.setInternalState(fragment, "image", simpleDraweeView)
        Whitebox.setInternalState(fragment, "title", textView)
        Whitebox.setInternalState(fragment, "toDoReason", textView)
        Whitebox.setInternalState(fragment, "desc", htmlTextView)
        Whitebox.setInternalState(fragment, "license", textView)
        Whitebox.setInternalState(fragment, "coordinates", textView)
        Whitebox.setInternalState(fragment, "seeMore", textView)
        Whitebox.setInternalState(fragment, "uploadedDate", textView)
        Whitebox.setInternalState(fragment, "mediaCaption", textView)
        Whitebox.setInternalState(fragment, "captionLayout", linearLayout)
        Whitebox.setInternalState(fragment, "depictsLayout", linearLayout)
        Whitebox.setInternalState(fragment, "delete", delete)
        Whitebox.setInternalState(fragment, "depictionContainer", linearLayout)
        Whitebox.setInternalState(fragment, "toDoLayout", linearLayout)
        Whitebox.setInternalState(fragment, "authorLayout", linearLayout)
        Whitebox.setInternalState(fragment, "showCaptionAndDescriptionContainer", linearLayout)
        Whitebox.setInternalState(fragment, "editDescription", button)
        Whitebox.setInternalState(fragment, "depictEditButton", button)
        Whitebox.setInternalState(fragment, "categoryEditButton", button)
        Whitebox.setInternalState(fragment, "categoryContainer", linearLayout)
        Whitebox.setInternalState(fragment, "progressBarDeletion", progressBarDeletion)
        Whitebox.setInternalState(fragment, "progressBarEditCategory", progressBarDeletion)
        Whitebox.setInternalState(fragment, "mediaDiscussion", textView)
        Whitebox.setInternalState(fragment, "locationManager", locationManager)

        `when`(simpleDraweeView.hierarchy).thenReturn(genericDraweeHierarchy)
        val map = HashMap<String, String>()
        map[Locale.getDefault().language] = ""
        `when`(media.descriptions).thenReturn(map)

        doReturn(mockSharedPreferences).`when`(mockContext).getSharedPreferences(anyString(), anyInt())
        doReturn(mockSharedPreferencesEditor).`when`(mockSharedPreferences).edit()
        doReturn(mockSharedPreferencesEditor).`when`(mockSharedPreferencesEditor).putInt(anyString(), anyInt())
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
    fun testOnActivityResultLocationPickerActivity() {
        fragment.onActivityResult(REQUEST_CODE, Activity.RESULT_CANCELED, intent)
    }

    @Test
    @Throws(Exception::class)
    fun `test OnActivity Result Cancelled LocationPickerActivity`() {
        fragment.onActivityResult(REQUEST_CODE, Activity.RESULT_CANCELED, intent)
    }

    @Test
    @Throws(Exception::class)
    fun `test OnActivity Result Cancelled DescriptionEditActivity`() {
        fragment.onActivityResult(REQUEST_CODE, Activity.RESULT_OK, intent)
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
        fragment.launchZoomActivity(view)
    }

    @Test
    @Throws(Exception::class)
    fun testOnUpdateCoordinatesClickedCurrentLocationNull() {
        `when`(media.coordinates).thenReturn(null)
        `when`(locationManager.lastLocation).thenReturn(null)
        `when`(applicationKvStore.getString(LAST_LOCATION)).thenReturn("37.773972,-122.431297")
        fragment.onUpdateCoordinatesClicked()
        Mockito.verify(media, Mockito.times(1)).coordinates
        Mockito.verify(locationManager, Mockito.times(1)).lastLocation
        val shadowActivity: ShadowActivity = shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shadowIntent: ShadowIntent = shadowOf(startedIntent)
        Assert.assertEquals(shadowIntent.intentClass, LocationPickerActivity::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testOnUpdateCoordinatesClickedNotNullValue() {
        `when`(media.coordinates).thenReturn(LatLng(-0.000001, -0.999999, 0f))
        `when`(applicationKvStore.getString(LAST_LOCATION)).thenReturn("37.773972,-122.431297")
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
        `when`(locationManager.lastLocation).thenReturn(LatLng(-0.000001, -0.999999, 0f))
        `when`(applicationKvStore.getString(LAST_LOCATION)).thenReturn("37.773972,-122.431297")

        fragment.onUpdateCoordinatesClicked()
        Mockito.verify(locationManager, Mockito.times(3)).lastLocation
        val shadowActivity: ShadowActivity = shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shadowIntent: ShadowIntent = shadowOf(startedIntent)
        Assert.assertEquals(shadowIntent.intentClass, LocationPickerActivity::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testOnResume() {
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
        val layoutListener = mock(ViewTreeObserver.OnGlobalLayoutListener::class.java)
        Whitebox.setInternalState(fragment, "layoutListener", layoutListener)
        fragment.onDestroyView()
    }

    @Test
    @Throws(Exception::class)
    fun testExtractDescription() {
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "extractDescription",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "")
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescription() {
        `when`(media.filename).thenReturn("")
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "getDescription"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithComma() {
        `when`(media.filename).thenReturn("")
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod("getDescriptions", String::class.java)
        method.isAccessible = true
        val s = "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|description={{en|1=Antique cash register in a cafe, Darjeeling}}\n" +
                "|date=2017-05-17 17:07:26\n" +
                "|source={{own}}\n" +
                "|author=[[User:Subhrajyoti07|Subhrajyoti07]]\n" +
                "|permission=\n" +
                "|other versions=\n" +
                "}}\n" +
                "{{Location|27.043186|88.267003}}\n" +
                "{{Assessments|featured=1}}"
        val map = linkedMapOf("en" to "Antique cash register in a cafe, Darjeeling")
        Assert.assertEquals(map, method.invoke(fragment, s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithNestedBrackets() {
        `when`(media.filename).thenReturn("")
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod("getDescriptions", String::class.java)
        method.isAccessible = true
        val s = "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|description={{en|1=[[:en:Fitzrovia Chapel|Fitzrovia Chapel]] ceiling<br/>\n" +
                "{{On Wikidata|Q17549757}}}}\n" +
                "|date=2017-09-17 13:09:39\n" +
                "|source={{own}}\n" +
                "|author=[[User:Colin|Colin]]\n" +
                "|permission=\n" +
                "|other versions=\n" +
                "|Other fields = {{Credit line |Author = © [[User:Colin]] | Other = Wikimedia Commons |License = CC-BY-SA-4.0}}\n" +
                "}}\n" +
                "{{Location|51.519003|-0.138353}}\n" +
                "{{Assessments|featured=1}}"
        val map = linkedMapOf("en" to "[[:en:Fitzrovia Chapel|Fitzrovia Chapel]] ceiling<br/>\n{{On Wikidata|Q17549757}}")
        Assert.assertEquals(map, method.invoke(fragment, s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithInvalidLanguageCode() {
        `when`(media.filename).thenReturn("")
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod("getDescriptions", String::class.java)
        method.isAccessible = true
        val s = "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|description={{en|1=[[:en:Fitzrovia Chapel|Fitzrovia Chapel]] ceiling<br/>\n" +
                "}}{{Listed building England|1223496}}\n" +
                "|date=2017-09-17 13:09:39\n" +
                "|source={{own}}\n" +
                "|author=[[User:Colin|Colin]]\n" +
                "|permission=\n" +
                "|other versions=\n" +
                "|Other fields = {{Credit line |Author = © [[User:Colin]] | Other = Wikimedia Commons |License = CC-BY-SA-4.0}}\n" +
                "}}\n" +
                "{{Location|51.519003|-0.138353}}\n" +
                "{{Assessments|featured=1}}"
        val map = linkedMapOf("en" to "[[:en:Fitzrovia Chapel|Fitzrovia Chapel]] ceiling<br/>\n")
        Assert.assertEquals(map, method.invoke(fragment, s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithSpaces() {
        `when`(media.filename).thenReturn("")
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod("getDescriptions", String::class.java)
        method.isAccessible = true
        val s = "=={{int:filedesc}}==\n" +
                "{{Artwork\n" +
                " |artist = {{Creator:Filippo Peroni}} Restored by {{Creator:Adam Cuerden}}\n" +
                " |author = \n" +
                " |title = Ricchi giardini nel Palazzo di Monforte a Palermo\n" +
                " |description = {{en|''Ricchi giardini nel Palazzo di Monforte a Palermo'', set design for ''I Vespri siciliani'' act 5 (undated).}} {{it|''Ricchi giardini nel Palazzo di Monforte a Palermo'', bozzetto per ''I Vespri siciliani'' atto 5 (s.d.).}}\n" +
                " |date = {{between|1855|1878}} (Premiére of the opera and death of the artist, respectively)\n" +
                " |medium = {{technique|watercolor|and=tempera|and2=|over=paper}}\n" +
                " |dimensions = {{Size|unit=mm|height=210|width=270}}\n" +
                " |institution = {{Institution:Archivio Storico Ricordi}}\n" +
                " |department = \n" +
                " |place of discovery = \n" +
                " |object history = \n" +
                " |exhibition history = \n" +
                " |credit line = \n" +
                " |inscriptions = \n" +
                " |notes = \n" +
                " |accession number = ICON000132\n" +
                " |place of creation = \n" +
                " |source = [https://www.archivioricordi.com/chi-siamo/glam-archivio-ricordi/#/ Archivio Storico Ricordi], [https://www.digitalarchivioricordi.com/it/works/display/108/Vespri_Siciliani__I Collezione Digitale Ricordi]\n" +
                " |permission={{PermissionTicket|id=2022031410007974|user=Ruthven}} \n" +
                " |other_versions = \n" +
                "* [[:File:Ricchi giardini nel Palazzo di Monforte a Palermo, bozzetto di Filippo Peroni per I Vespri siciliani (s.d.) - Archivio Storico Ricordi ICON000132 - Restoration.jpg]] - Restoration (JPEG)\n" +
                "* [[:File:Ricchi giardini nel Palazzo di Monforte a Palermo, bozzetto di Filippo Peroni per I Vespri siciliani (s.d.) - Archivio Storico Ricordi ICON000132 - Restoration.png]] - Restoration (PNG)\n" +
                "* [[:File:Ricchi giardini nel Palazzo di Monforte a Palermo, bozzetto di Filippo Peroni per I Vespri siciliani (s.d.) - Archivio Storico Ricordi ICON000132.jpg]] - Original (JPEG)\n" +
                " |references = \n" +
                " |wikidata = \n" +
                "}}"
        val map = linkedMapOf("en" to "''Ricchi giardini nel Palazzo di Monforte a Palermo'', set design for ''I Vespri siciliani'' act 5 (undated).",
        "it" to "''Ricchi giardini nel Palazzo di Monforte a Palermo'', bozzetto per ''I Vespri siciliani'' atto 5 (s.d.).")
        Assert.assertEquals(map, method.invoke(fragment, s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionsWithLongSpaces() {
        `when`(media.filename).thenReturn("")
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod("getDescriptions", String::class.java)
        method.isAccessible = true
        val s = "=={{int:filedesc}}==\n" +
                "{{Information\n" +
                "|Description   ={{en|1=The interior of Sacred Heart RC Church, Wimbledon, London.}}\n" +
                "|Source        ={{own}}\n" +
                "|Author        =[[User:Diliff|Diliff]]\n" +
                "|Date          =2015-02-17\n" +
                "|Permission    ={{Diliff/Licensing}}\n" +
                "|other_versions=\n" +
                "}}"
        val map = linkedMapOf("en" to "The interior of Sacred Heart RC Church, Wimbledon, London.")
        Assert.assertEquals(map, method.invoke(fragment, s))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptionList() {
        `when`(media.filename).thenReturn("")
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "getDescriptionList"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCaptions() {
        `when`(media.captions).thenReturn(mapOf(Pair("a", "b")))
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "getCaptions"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCaptionsCaseEmpty() {
        `when`(media.captions).thenReturn(mapOf())
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "getCaptions"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUpCaptionAndDescriptionLayout() {
        `when`(media.filename).thenReturn("")
        val field: Field =
            MediaDetailFragment::class.java.getDeclaredField("descriptionHtmlCode")
        field.isAccessible = true
        field.set(fragment, null)
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "setUpCaptionAndDescriptionLayout"
        )
        method.isAccessible = true
        method.invoke(fragment)
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
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyCoordinates",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyCoordinates() {
        `when`(media.coordinates).thenReturn(LatLng(-0.000001, -0.999999, 0f))
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyCoordinates",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyUploadedDateCaseNull() {
        `when`(media.dateUploaded).thenReturn(null)
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyUploadedDate",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyUploadedDateCaseNonNull() {
        `when`(media.dateUploaded).thenReturn(Date(2000, 1, 1))
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyUploadedDate",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyLicenseCaseNull() {
        `when`(media.license).thenReturn(null)
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyLicense",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyLicenseCaseNonNull() {
        `when`(media.license).thenReturn("licence")
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyLicense",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyDiscussion() {
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyDiscussion",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "mock")
    }

    @Test
    @Throws(Exception::class)
    fun testExtractCaptionDescription() {
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "extractCaptionDescription",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "mock")
    }

    @Test
    @Throws(Exception::class)
    fun testGetDescriptions() {
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "getDescriptions",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "mock")
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyCaptionCaseEmpty() {
        `when`(media.captions).thenReturn(mapOf(Pair("a", "")))
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyCaption",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyCaptionCaseNonEmpty() {
        `when`(media.captions).thenReturn(mapOf(Pair("a", "b")))
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyCaption",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testPrettyCaption() {
        `when`(media.captions).thenReturn(mapOf())
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "prettyCaption",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testSetupImageView() {
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "setupImageView"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDiscussionLoaded() {
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "onDiscussionLoaded",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "")
    }

    @Test
    @Throws(Exception::class)
    fun testOnDeleteClickedNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val spinner = mock(Spinner::class.java)
        `when`(media.imageUrl).thenReturn("test@example.com")
        `when`(spinner.selectedItemPosition).thenReturn(0)
        `when`(reasonListEnglishMappings?.get(spinner.selectedItemPosition)).thenReturn("TESTING")
        `when`(applicationKvStore.getBoolean(String.format(MediaDetailFragment.NOMINATING_FOR_DELETION_MEDIA,media.imageUrl
                ))).thenReturn(true)
        doReturn(Single.just(true)).`when`(deleteHelper).makeDeletion(ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())

        doReturn(Single.just("")).`when`(reasonBuilder).getReason(ArgumentMatchers.any(), ArgumentMatchers.any())

        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "onDeleteClicked",
            Spinner::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, spinner)
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
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "displayMediaDetails"
        )
        method.isAccessible = true
        method.invoke(fragment)
        verify(media, times(4)).filename
    }

    @Test
    @Throws(Exception::class)
    fun testGotoCategoryEditor() {
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "gotoCategoryEditor",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "[[Category:Test]]")
    }

    @Test
    @Throws(Exception::class)
    fun testOnMediaRefreshed() {
        val method: Method = MediaDetailFragment::class.java.getDeclaredMethod(
            "onMediaRefreshed",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }
    
    @Test
    fun testOnImageBackgroundChangedWithDifferentColor() {
        val spyFragment = spy(fragment)
        val color = 0xffffff
        doReturn(mockContext).`when`(spyFragment).context
        doReturn(-1).`when`(mockSharedPreferences).getInt(anyString(), anyInt())

        spyFragment.onImageBackgroundChanged(color)

        verify(simpleDraweeView, times(1)).setBackgroundColor(color) 
        verify(mockSharedPreferencesEditor, times(1)).putInt(anyString(), anyInt())
    }


    @Test
    fun testOnImageBackgroundChangedWithSameColor() {
        val spyFragment = spy(fragment)
        val color = 0
        doReturn(mockContext).`when`(spyFragment).context
        doReturn(color).`when`(mockSharedPreferences).getInt(anyString(), anyInt())

        spyFragment.onImageBackgroundChanged(color)
        verify(simpleDraweeView, never()).setBackgroundColor(anyInt())
        verify(mockSharedPreferencesEditor, never()).putInt(anyString(), anyInt())
    }
}
