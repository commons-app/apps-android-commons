package fr.free.nrw.commons.filepicker;

/**
 * Provides abstract methods which are overridden while handling Contribution Results
 * inside the ContributionsController 
 */
public abstract class DefaultCallback implements FilePicker.Callbacks {

    @Override
    public void onImagePickerError(Exception e, FilePicker.ImageSource source, int type) {
    }

    @Override
    public void onCanceled(FilePicker.ImageSource source, int type) {
    }
}