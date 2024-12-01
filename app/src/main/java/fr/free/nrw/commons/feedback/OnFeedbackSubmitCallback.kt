package fr.free.nrw.commons.feedback;

import fr.free.nrw.commons.feedback.model.Feedback;

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
