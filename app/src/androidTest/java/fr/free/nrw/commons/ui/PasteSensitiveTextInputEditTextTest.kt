package fr.free.nrw.commons.ui

import android.content.Context
import android.util.AttributeSet
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasteSensitiveTextInputEditTextTest {

    private var context: Context? = null
    private var textView: PasteSensitiveTextInputEditText? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        textView = PasteSensitiveTextInputEditText(context)
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