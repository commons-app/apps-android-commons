package fr.free.nrw.commons.quiz

import android.app.Activity
import android.widget.RadioButton
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class RadioGroupHelperUnitTest {
    private lateinit var radioGroupHelper: RadioGroupHelper

    @Mock
    private lateinit var radioButton: RadioButton

    @Mock
    private lateinit var radioButton1: RadioButton

    @Mock
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        radioGroupHelper = RadioGroupHelper()
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(radioGroupHelper)
    }

    @Test
    @Throws(Exception::class)
    fun constructor1() {
        radioGroupHelper = RadioGroupHelper(radioButton, radioButton1)
    }

    @Test
    @Throws(Exception::class)
    fun constructor2() {
        radioGroupHelper = RadioGroupHelper(activity)
    }
}
