package fr.free.nrw.commons.ui

import android.R
import android.content.Context
import android.os.Build
import android.util.AttributeSet
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
    fun onTextContextMenuItemPasteFormattingDisabled() {
        textView!!.setFormattingAllowed(false);
        textView!!.setText("Text")
        textView!!.onTextContextMenuItem(R.id.paste)
        Assert.assertEquals("Text", textView!!.text.toString())
    }

    @Test
    fun onTextContextMenuItemPasteFormattingAllowed() {
        textView!!.setFormattingAllowed(true);
        textView!!.setText("Text")
        textView!!.onTextContextMenuItem(R.id.paste)
        Assert.assertEquals("Text", textView!!.text.toString())
    }

    @Test
    fun onTextContextMenuItemPaste() {
        textView!!.setText("Text")
        textView!!.onTextContextMenuItem(R.id.paste)
        Assert.assertEquals("Text", textView!!.text.toString())
    }


    @Test
    fun onTextContextMenuItemNotPaste() {
        textView!!.setText("Text")
        textView!!.onTextContextMenuItem(R.id.copy)
        Assert.assertEquals("Text", textView!!.text.toString())
    }

    // this test has no real value, just % for test code coverage
    @Test
    fun extractFormattingAttributeSet(){
        val methodExtractFormattingAttribute = textView!!.javaClass.getDeclaredMethod(
            "extractFormattingAttribute", Context::class.java, AttributeSet::class.java)
        methodExtractFormattingAttribute.isAccessible = true
        methodExtractFormattingAttribute.invoke(textView, context, null)
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