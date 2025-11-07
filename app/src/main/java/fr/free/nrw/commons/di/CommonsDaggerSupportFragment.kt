package fr.free.nrw.commons.di

import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable

/**
 * Base class for fragments that previously used Dagger Android injection.
 *
 * NOTE: This class is DEPRECATED with Hilt. Fragments should use @AndroidEntryPoint annotation instead.
 * This class is kept as a simple Fragment extension for backward compatibility,
 * but all injection functionality has been removed.
 *
 * Fragments extending this class should add @AndroidEntryPoint annotation to enable Hilt injection.
 */
abstract class CommonsDaggerSupportFragment : Fragment() {
    // Kept for backward compatibility - fragments that used this can continue to use it
    protected open var compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}

