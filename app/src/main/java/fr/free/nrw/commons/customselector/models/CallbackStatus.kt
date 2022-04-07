package fr.free.nrw.commons.customselector.models

/**
 * sealed class Callback Status.
 * Current status of the device image query.
 */
sealed class CallbackStatus {
    /**
    IDLE : The callback is idle , doing nothing.
     */
    object IDLE : CallbackStatus()

    /**
    FETCHING : Fetching images.
     */
    object FETCHING : CallbackStatus()

    /**
    SUCCESS : Success fetching images.
     */
    object SUCCESS : CallbackStatus()
}