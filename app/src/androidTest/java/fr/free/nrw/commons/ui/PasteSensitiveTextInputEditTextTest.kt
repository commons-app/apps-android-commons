package fr.free.nrw.commons.ui

import android.R
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception
import kotlin.Throws

@RunWith(AndroidJUnit4::class)
class PasteSensitiveTextInputEditTextTest {

    private var context: Context? = null
    private var textView: PasteSensitiveTextInputEditText? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        textView = PasteSensitiveTextInputEditText(context)
    }

    @Test
    fun onTextContextMenuItem() {
        textView!!.setText("Text")
        textView!!.onTextContextMenuItem(R.id.paste)
        Assert.assertEquals("Text", textView!!.text.toString())
    }

    @Test
    @Throws(Exception::class)
    fun setFormattingAllowed() {
        val fieldFormattingAllowed = textView!!.javaClass.getDeclaredField("formattingAllowed")
        fieldFormattingAllowed.isAccessible = true
        textView!!.setFormattingAllowed(true)
        Assert.assertTrue(fieldFormattingAllowed.getBoolean(textView))
        textView!!.setFormattingAllowed(false)
        Assert.assertFalse(fieldFormattingAllowed.getBoolean(textView))
    }
}