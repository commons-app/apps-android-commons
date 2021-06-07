package fr.free.nrw.commons.customselector.model

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