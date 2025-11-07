package fr.free.nrw.commons.di

import androidx.appcompat.app.AppCompatActivity

/**
 * Base class for activities that previously used Dagger Android injection.
 *
 * NOTE: This class is DEPRECATED with Hilt. Activities should use @AndroidEntryPoint annotation instead.
 * This class is kept as a simple AppCompatActivity extension for backward compatibility,
 * but all injection functionality has been removed.
 *
 * Activities extending this class should add @AndroidEntryPoint annotation to enable Hilt injection.
 */
abstract class CommonsDaggerAppCompatActivity : AppCompatActivity()
