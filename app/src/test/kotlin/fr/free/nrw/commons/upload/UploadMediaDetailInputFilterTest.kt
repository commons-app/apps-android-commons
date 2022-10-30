package fr.free.nrw.commons.upload

import android.text.SpannableStringBuilder
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class UploadMediaDetailInputFilterTest {

    @Test
    fun testFilterGeneric() {
        val filter = UploadMediaDetailInputFilter()
        val filters = arrayOf(filter)
        val destination = SpannableStringBuilder("")
        destination.filters = filters

        val test: CharSequence = "test"
        val expected = "test"
        destination.insert(0, test)
        Assert.assertEquals(destination.toString(), expected)
    }

    @Test
    fun testFilterUnusualSpaces() {
        val builder = SpannableStringBuilder("")
        builder.filters = arrayOf(UploadMediaDetailInputFilter())

        //All unusual space characters
        val tests = intArrayOf(0x00A0, 0x1680, 0x180E, 0x2000, 0x2005, 0x200B, 0x2028, 0x2029, 0x202F, 0x205F)
        for (test: Int in tests) {
            builder.insert(0, String(Character.toChars(test)))
            Assert.assertEquals(builder.toString(), "")
            builder.clear()
        }
    }

    @Test
    fun testFilterBiDiOverrides() {
        val builder = SpannableStringBuilder("")
        builder.filters = arrayOf(UploadMediaDetailInputFilter())

        //Sample of BiDI override characters
        val tests = intArrayOf(0x202A, 0x202B, 0x202C, 0x202D, 0x202E)
        for (test: Int in tests) {
            builder.insert(0, String(Character.toChars(test)))
            Assert.assertEquals(builder.toString(), "")
            builder.clear()
        }
    }

    @Test
    fun testFilterControlCharacters() {
        val builder = SpannableStringBuilder("")
        builder.filters = arrayOf(UploadMediaDetailInputFilter())

        //Sample of control characters
        val tests = intArrayOf(0x00, 0x08, 0x10, 0x18, 0x1F, 0x7F)
        for (test: Int in tests) {
            builder.insert(0, String(Character.toChars(test)))
            Assert.assertEquals(builder.toString(), "")
            builder.clear()
        }
    }

    @Test
    fun testFilterByteOrderMark() {
        val builder = SpannableStringBuilder("")
        builder.filters = arrayOf(UploadMediaDetailInputFilter())

        builder.insert(0, String(Character.toChars(0xFEFF)))
        Assert.assertEquals(builder.toString(), "")
        builder.clear()
    }

    @Test
    fun testFilterSoftHyphen() {
        val builder = SpannableStringBuilder("")
        builder.filters = arrayOf(UploadMediaDetailInputFilter())

        builder.insert(0, String(Character.toChars(0x00AD)))
        Assert.assertEquals(builder.toString(), "")
        builder.clear()
    }

    @Test
    fun testFilterSpecials() {
        val builder = SpannableStringBuilder("")
        builder.filters = arrayOf(UploadMediaDetailInputFilter())

        //Sample of surrogate and special characters
        val tests = intArrayOf(0xE000, 0xE63F, 0xEC7E, 0xF2BD, 0xF8FF, 0xFFF0, 0xFFF4, 0xFFFC, 0xFFFF)
        for (test: Int in tests) {
            builder.insert(0, String(Character.toChars(test)))
            Assert.assertEquals(builder.toString(), "")
            builder.clear()
        }
    }

    @Test
    fun testFilterNonBasicPlane() {
        val builder = SpannableStringBuilder("")
        builder.filters = arrayOf(UploadMediaDetailInputFilter())

        //Sample of characters over 5 hex places not in the Han set
        val testsExclude = intArrayOf(0x1FFFF, 0x44444, 0xFFFFF)
        for (test: Int in testsExclude) {
            builder.insert(0, String(Character.toChars(test)))
            Assert.assertEquals(builder.toString(), "")
            builder.clear()
        }

        //Sample of characters over 5 hex places in the Han set
        val testsInclude = intArrayOf(0x20000, 0x2B740, 0x2F800)
        val expected = SpannableStringBuilder("")
        for (test: Int in testsInclude) {
            builder.insert(0, String(Character.toChars(test)))
            expected.insert(0, String(Character.toChars(test)))
        }
        Assert.assertEquals(builder.toString(), expected.toString())
    }
}