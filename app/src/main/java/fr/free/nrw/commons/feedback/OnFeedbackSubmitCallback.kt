package fr.free.nrw.commons.feedback

import fr.free.nrw.commons.feedback.model.Feedback

interface OnFeedbackSubmitCallback {
    fun onFeedbackSubmit(feedback: Feedback)
}