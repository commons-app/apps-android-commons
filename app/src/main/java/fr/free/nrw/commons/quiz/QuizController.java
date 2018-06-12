package fr.free.nrw.commons.quiz;

import java.util.ArrayList;

import butterknife.BindView;

/**
 * controls the quiz in the Activity
 */
public class QuizController {

    ArrayList<QuizQuestion> quiz = new ArrayList<QuizQuestion>();

    public void initialize(){
        QuizQuestion q1 = new QuizQuestion(1,
                "Which License can be used to donate this image to public domain?",
                true);
        quiz.add(q1);
    }

    public ArrayList<QuizQuestion> getQuiz() {
        return quiz;
    }
}
