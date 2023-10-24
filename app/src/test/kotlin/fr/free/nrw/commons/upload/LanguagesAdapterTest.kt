package fr.free.nrw.commons.upload

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.ConfigurationCompat
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.language.AppLanguageLookUpTable
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LanguagesAdapterTest {

    private lateinit var context: Context

    @Mock
    private lateinit var selectedLanguages: HashMap<Integer, String>
    @Mock
    private lateinit var parent: ViewGroup

    private lateinit var languageNamesList: List<String>
    private lateinit var languageCodesList: List<String>
    private lateinit var language: AppLanguageLookUpTable

    private lateinit var languagesAdapter: LanguagesAdapter
    private lateinit var convertView: View
    private var selectLanguages: HashMap<Integer, String> = HashMap()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        language = AppLanguageLookUpTable(context)
        convertView = LayoutInflater.from(context)
            .inflate(R.layout.row_item_languages_spinner, null) as View

        languageNamesList = language.localizedNames
        languageCodesList = language.codes

        languagesAdapter = LanguagesAdapter(context, selectedLanguages)
    }

    @Test
    @Throws(Exception::class)
    fun testOnGetView() {
        languagesAdapter = LanguagesAdapter(context, selectedLanguages)
        `when`(selectedLanguages.values).thenReturn(Collections.emptyList())
        Assert.assertEquals(languagesAdapter.getView(0, convertView, parent), convertView)
    }

    @Test
    fun testGetCount() {
        Assertions.assertEquals(languageCodesList.size, languagesAdapter.count)
    }

    @Test
    fun testGetLanguageCode() {
        languagesAdapter = LanguagesAdapter(context, selectedLanguages)
        Assertions.assertEquals(languagesAdapter.getLanguageCode(0), languageCodesList[0])
    }

    @Test
    fun testGetIndexOfUserDefaultLocale() {
        languagesAdapter = LanguagesAdapter(context, selectedLanguages)
        Assertions.assertEquals(ConfigurationCompat.getLocales(context.resources.configuration)[0]?.let {
            languageCodesList.indexOf(
                it.language)
        }, languagesAdapter.getIndexOfUserDefaultLocale(context))
    }

    @Test
    fun testSelectLanguageNotEmpty() {
        selectLanguages[Integer(0)] = "es"
        selectLanguages[Integer(1)] = "de"
        languagesAdapter = LanguagesAdapter(context, selectLanguages)

        Assertions.assertEquals(false, languagesAdapter.isEnabled(languagesAdapter.getIndexOfLanguageCode("es")))
        Assertions.assertEquals(false, languagesAdapter.isEnabled(languagesAdapter.getIndexOfLanguageCode("de")))
    }

    @Test
    fun testFilterEmpty() {
        languagesAdapter = LanguagesAdapter(context, selectedLanguages)
        languagesAdapter.filter.filter("")
        Assertions.assertEquals(languageCodesList.size, languagesAdapter.count)
    }

    @Test
    fun testFilterNonEmpty() {
        languagesAdapter = LanguagesAdapter(context, selectedLanguages)
        val constraint = "spa"
        languagesAdapter.filter.filter(constraint)
        val length: Int = languageNamesList.size
        val defaultlanguagecode = ConfigurationCompat.getLocales(context.resources.configuration)[0]?.let {
            languageCodesList.indexOf(
                it.language)
        }
        var i = 0
        var s = 0
        while (i < length) {
            val key: String = language.codes[i]
            val value: String = language.localizedNames[i]
            if(value.contains(constraint, true) || Locale(key).getDisplayName(
                    Locale(language.codes[defaultlanguagecode!!])).contains(constraint, true))
                s++
            i++
        }
        Assertions.assertEquals(s, languagesAdapter.count)
    }

}