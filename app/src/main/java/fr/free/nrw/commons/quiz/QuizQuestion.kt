package fr.free.nrw.commons.quiz

import android.net.Uri

/**
 * class contains information about all the quiz questions
 */
class QuizQuestion internal constructor(var questionNumber: Int, var question: String, private var url: String, var isAnswer: Boolean, var answerMessage: String) {
    fun getUrl(): Uri {
        return Uri.parse(url)
    }

    fun setUrl(url: String) {
        this.url = url
    }

}