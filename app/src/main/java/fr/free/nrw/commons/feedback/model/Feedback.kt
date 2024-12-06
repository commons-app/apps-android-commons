package fr.free.nrw.commons.feedback.model

/**
 * Pojo class for storing information that are required while uploading a feedback
 */
data class Feedback (
    // Version of app
    var version : String? = null,

    // API level of user's phone
    var apiLevel: String? = null,

    // Title/Description entered by user
    var title: String? = null,

    // Android version of user's device
    var androidVersion: String? = null,

    // Device Model of user's device
    var deviceModel: String? = null,

    // Device manufacturer name
    var deviceManufacturer: String? = null,

    // Device name stored on user's device
    var device: String? = null,

    // network type user is having (Ex: Wifi)
    var networkType: String? = null
)