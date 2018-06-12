package fr.free.nrw.commons.quiz;

import android.content.Context;

import java.util.ArrayList;

import butterknife.BindView;
import fr.free.nrw.commons.R;

/**
 * controls the quiz in the Activity
 */
public class QuizController {

    ArrayList<QuizQuestion> quiz = new ArrayList<QuizQuestion>();

    public void initialize(Context context){
        QuizQuestion q1 = new QuizQuestion(1,
                context.getResources().getString(R.string.quiz_question_string),
                "https://i.imgur.com/0fMYcpM.jpg",
                true);
        quiz.add(q1);
        QuizQuestion q2 = new QuizQuestion(2,
                context.getResources().getString(R.string.quiz_question_string),
                "https://upload.wikimedia.org/wikipedia/commons/1/15/Taj_Mahal-03.jpg",
                true);
        quiz.add(q2);
        QuizQuestion q3 = new QuizQuestion(3,
                context.getResources().getString(R.string.quiz_question_string),
                "https://upload.wikimedia.org/wikipedia/commons/5/51/HouseBuildingInNorthernVietnam.jpg",
                true);
        quiz.add(q3);
        QuizQuestion q4 = new QuizQuestion(4,
                context.getResources().getString(R.string.quiz_question_string),
                "https://upload.wikimedia.org/wikipedia/commons/5/51/HouseBuildingInNorthernVietnam.jpg",
                true);
        quiz.add(q4);

        QuizQuestion q5 = new QuizQuestion(5,
                context.getResources().getString(R.string.quiz_question_string),
                "https://upload.wikimedia.org/wikipedia/commons/5/51/HouseBuildingInNorthernVietnam.jpg",
                true);
        quiz.add(q5);

    }

    public ArrayList<QuizQuestion> getQuiz() {
        return quiz;
    }
}
