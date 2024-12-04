package fr.free.nrw.commons.filepicker

/**
 * Provides abstract methods which are overridden while handling Contribution Results
 * inside the ContributionsController
 */
abstract class DefaultCallback: FilePicker.Callbacks {

    override fun onImagePickerError(e: Exception, source: FilePicker.ImageSource, type: Int) {}

    override fun onCanceled(source: FilePicker.ImageSource, type: Int) {}
}