package fr.free.nrw.commons.di

import android.content.BroadcastReceiver

/**
 * Base class for broadcast receivers that previously used Dagger Android injection.
 *
 * NOTE: This class is DEPRECATED with Hilt. BroadcastReceivers should use constructor injection
 * or retrieve dependencies via EntryPoint interfaces.
 * This class is kept as a simple BroadcastReceiver extension for backward compatibility,
 * but all injection functionality has been removed.
 */
abstract class CommonsDaggerBroadcastReceiver : BroadcastReceiver()
