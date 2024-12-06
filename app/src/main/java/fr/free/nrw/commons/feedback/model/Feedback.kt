package fr.free.nrw.commons.feedback.model

/**
 * Pojo class for storing information that are required while uploading a feedback
 */
data class Feedback (
    // Version of app
    val version : String? = null,

    // API level of user's phone
    val apiLevel: String? = null,

    // Title/Description entered by user
    val title: String? = null,

    // Android version of user's device
    val androidVersion: String? = null,

    // Device Model of user's device
    val deviceModel: String? = null,

    // Device manufacturer name
    val deviceManufacturer: String? = null,

    // Device name stored on user's device
    val device: String? = null,

    // network type user is having (Ex: Wifi)
    val networkType: String? = null
)