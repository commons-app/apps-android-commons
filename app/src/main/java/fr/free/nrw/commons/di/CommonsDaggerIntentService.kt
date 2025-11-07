package fr.free.nrw.commons.di

import android.app.IntentService

/**
 * Base class for intent services that previously used Dagger Android injection.
 *
 * NOTE: This class is DEPRECATED with Hilt. IntentServices should use WorkManager with Hilt Workers instead.
 * This class is kept as a simple IntentService extension for backward compatibility,
 * but all injection functionality has been removed.
 */
abstract class CommonsDaggerIntentService(name: String?) : IntentService(name)

