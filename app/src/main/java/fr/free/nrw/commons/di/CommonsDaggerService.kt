package fr.free.nrw.commons.di

import android.app.Service

/**
 * Base class for services that previously used Dagger Android injection.
 *
 * NOTE: This class is DEPRECATED with Hilt. Services should use @AndroidEntryPoint annotation instead.
 * This class is kept as a simple Service extension for backward compatibility,
 * but all injection functionality has been removed.
 *
 * Services extending this class should add @AndroidEntryPoint annotation to enable Hilt injection.
 */
abstract class CommonsDaggerService : Service()

