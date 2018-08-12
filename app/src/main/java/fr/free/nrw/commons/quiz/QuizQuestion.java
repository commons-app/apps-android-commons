package fr.free.nrw.commons.quiz;

import android.net.Uri;

/**
 * class contains information about all the quiz questions
 */
public class QuizQuestion {
    private int questionNumber;
    private String question;
    private boolean answer;
    private String url;
    private String answerMessage;

    QuizQuestion(int questionNumber, String question, String url, boolean answer, String answerMessage){
        this.questionNumber = questionNumber;
        this.question = question;
        this.url = url;
        this.answer = answer;
        this.answerMessage = answerMessage;
    }

    public Uri getUrl() {
        return Uri.parse(url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isAnswer() {
        return answer;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public String getQuestion() {
        return question;
    }

    public void setAnswer(boolean answer) {
        this.answer = answer;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getAnswerMessage() {
        return answerMessage;
    }

    public void setAnswerMessage(String answerMessage) {
        this.answerMessage = answerMessage;
    }
}
