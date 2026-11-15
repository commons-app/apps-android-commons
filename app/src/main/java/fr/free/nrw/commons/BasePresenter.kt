package fr.free.nrw.commons

/**
 * Base presenter, enforcing contracts to attach and detach view
 */
interface BasePresenter<T> {
    fun onAttachView(view: T)

    fun onDetachView()
}
