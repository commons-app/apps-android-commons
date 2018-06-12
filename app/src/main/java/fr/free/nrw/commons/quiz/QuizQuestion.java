package fr.free.nrw.commons.quiz;

/**
 * class contains information about all the quiz questions
 */
public class QuizQuestion {
    private int questionNumber;
    private String question;
    private boolean answer;

    QuizQuestion( int questionNumber, String question, boolean answer){
        this.questionNumber = questionNumber;
        this.question = question;
        this.answer = answer;
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

}
