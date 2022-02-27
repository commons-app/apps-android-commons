package fr.free.nrw.commons.feedback;

/**
 * This interface is used to provide callback
 * from Feedback dialog whenever submit button is clicked
 */
public interface OnFeedbackSubmitCallback {

    /**
     * callback function, called when user clicks on submit
     */
    void onFeedbackSubmit(Feedback feedback);
}
