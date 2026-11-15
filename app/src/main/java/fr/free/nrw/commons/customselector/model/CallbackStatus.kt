package fr.free.nrw.commons.customselector.model

/**
 * sealed class Callback Status.
 * Current status of the device image query.
 */
sealed class CallbackStatus {
    /**
     IDLE : The callback is idle , doing nothing.
     */
    data object IDLE : CallbackStatus()

    /**
     FETCHING : Fetching images.
     */
    data object FETCHING : CallbackStatus()

    /**
     SUCCESS : Success fetching images.
     */
    data object SUCCESS : CallbackStatus()
}
