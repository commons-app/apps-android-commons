package fr.free.nrw.commons.filepicker;

public abstract class DefaultCallback implements FilePicker.Callbacks {

    @Override
    public void onImagePickerError(Exception e, FilePicker.ImageSource source, int type) {
    }

    @Override
    public void onCanceled(FilePicker.ImageSource source, int type) {
    }
}