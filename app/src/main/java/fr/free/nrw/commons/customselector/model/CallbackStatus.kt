package fr.free.nrw.commons.customselector.model

import android.telecom.Call

sealed class CallbackStatus {
    object IDLE : CallbackStatus()
    object FETCHING : CallbackStatus()
    object SUCCESS : CallbackStatus()
}