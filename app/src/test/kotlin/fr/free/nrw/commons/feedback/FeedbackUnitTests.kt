package fr.free.nrw.commons.feedback

import fr.free.nrw.commons.feedback.model.Feedback
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FeedbackUnitTests {
    private lateinit var feedback: Feedback

    @Before
    fun setup() {
        feedback = Feedback("123", "apiLevel", "title", "androidVersion", "deviceModel", "mfg", "deviceName", "wifi")
    }

    @Test
    fun testVersion() {
        feedback.version = "456"
        Assert.assertEquals(feedback.version, "456")
    }

    @Test
    fun testApiLevel() {
        Assert.assertEquals(feedback.apiLevel, "apiLevel")
        feedback.apiLevel = "29"
        Assert.assertEquals(feedback.apiLevel, "29")
    }

    @Test
    fun testTitle() {
        feedback.title = "myTitle"
        Assert.assertEquals(feedback.title, "myTitle")
    }

    @Test
    fun testAndroidVersion() {
        feedback.androidVersion = "PIE"
        Assert.assertEquals(feedback.androidVersion, "PIE")
    }

    @Test
    fun testDeviceModel() {
        feedback.deviceModel = "My Device"
        Assert.assertEquals(feedback.deviceModel, "My Device")
    }

    @Test
    fun testDeviceMfg() {
        feedback.deviceManufacturer = "MYCOMPANY"
        Assert.assertEquals(feedback.deviceManufacturer, "MYCOMPANY")
    }

    @Test
    fun testDeviceName() {
        feedback.device = "my_name"
        Assert.assertEquals(feedback.device, "my_name")
    }

    @Test
    fun testNetworkType() {
        feedback.networkType = "network"
        Assert.assertEquals(feedback.networkType, "network")
    }

}