package fr.free.nrw.commons.feedback

import fr.free.nrw.commons.feedback.model.Feedback
import org.junit.Before
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo

class FeedbackUnitTests {
    private lateinit var feedback: Feedback

    @Before
    fun setup() {
        feedback = Feedback("123", "apiLevel", "title", "androidVersion", "deviceModel", "mfg", "deviceName", "wifi")
    }

    @Test
    fun testVersion() {
        feedback.version = "456"
        assertThat(feedback.version, equalTo( "456"))
    }

    @Test
    fun testApiLevel() {
        assertThat(feedback.apiLevel, equalTo( "apiLevel"))
        feedback.apiLevel = "29"
        assertThat(feedback.apiLevel, equalTo( "29"))
    }

    @Test
    fun testTitle() {
        feedback.title = "myTitle"
        assertThat(feedback.title, equalTo( "myTitle"))
    }

    @Test
    fun testAndroidVersion() {
        feedback.androidVersion = "PIE"
        assertThat(feedback.androidVersion, equalTo( "PIE"))
    }

    @Test
    fun testDeviceModel() {
        feedback.deviceModel = "My Device"
        assertThat(feedback.deviceModel, equalTo( "My Device"))
    }

    @Test
    fun testDeviceMfg() {
        feedback.deviceManufacturer = "MYCOMPANY"
        assertThat(feedback.deviceManufacturer, equalTo( "MYCOMPANY"))
    }

    @Test
    fun testDeviceName() {
        feedback.device = "my_name"
        assertThat(feedback.device, equalTo( "my_name"))
    }

    @Test
    fun testNetworkType() {
        feedback.networkType = "network"
        assertThat(feedback.networkType, equalTo( "network"))
    }

}