package fr.free.nrw.commons.di

import fr.free.nrw.commons.auth.WikiAccountAuthenticatorService

/**
 * This Class Represents the Module for dependency injection (using dagger)
 * so, if a developer needs to add a new Service to the commons app
 * then that must be mentioned here to inject the dependencies
 *
 * NOTE: This module is DEPRECATED with Hilt. Services should use @AndroidEntryPoint instead.
 * This file is kept for reference but all functionality has been migrated to Hilt.
 * The @Module annotation has been removed to prevent Hilt build errors.
 */
@Suppress("unused")
abstract class ServiceBuilderModule {
    // All methods below are deprecated and non-functional
    // Services should use @AndroidEntryPoint annotation instead

    /*
    @ContributesAndroidInjector
    abstract fun bindWikiAccountAuthenticatorService(): WikiAccountAuthenticatorService
    */
}
