package fr.free.nrw.commons.quiz;

import android.content.Context;

import fr.free.nrw.commons.data.models.quiz.QuizQuestion;
import java.util.ArrayList;

import fr.free.nrw.commons.R;

/**
 * controls the quiz in the Activity
 */
public class QuizController {

    ArrayList<QuizQuestion> quiz = new ArrayList<>();

    private final String URL_FOR_SELFIE = "https://i.imgur.com/0fMYcpM.jpg";
    private final String URL_FOR_TAJ_MAHAL = "https://upload.wikimedia.org/wikipedia/commons/1/15/Taj_Mahal-03.jpg";
    private final String URL_FOR_BLURRY_IMAGE = "https://i.imgur.com/Kepb5jR.jpg";
    private final String URL_FOR_SCREENSHOT = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8b/Social_media_app_mockup_screenshot.svg/500px-Social_media_app_mockup_screenshot.svg.png";
    private final String URL_FOR_EVENT = "https://upload.wikimedia.org/wikipedia/commons/5/51/HouseBuildingInNorthernVietnam.jpg";

    public void initialize(Context context){
        QuizQuestion q1 = new QuizQuestion(1,
                context.getString(R.string.quiz_question_string),
                URL_FOR_SELFIE,
                false,
                context.getString(R.string.selfie_answer));
        quiz.add(q1);

        QuizQuestion q2 = new QuizQuestion(2,
                context.getString(R.string.quiz_question_string),
                URL_FOR_TAJ_MAHAL,
                true,
                context.getString(R.string.taj_mahal_answer));
        quiz.add(q2);

        QuizQuestion q3 = new QuizQuestion(3,
                context.getString(R.string.quiz_question_string),
                URL_FOR_BLURRY_IMAGE,
                false,
                context.getString(R.string.blurry_image_answer));
        quiz.add(q3);

        QuizQuestion q4 = new QuizQuestion(4,
                context.getString(R.string.quiz_screenshot_question),
                URL_FOR_SCREENSHOT,
                false,
                context.getString(R.string.screenshot_answer));
        quiz.add(q4);

        QuizQuestion q5 = new QuizQuestion(5,
                context.getString(R.string.quiz_question_string),
                URL_FOR_EVENT,
                true,
                context.getString(R.string.construction_event_answer));
        quiz.add(q5);

    }

    public ArrayList<QuizQuestion> getQuiz() {
        return quiz;
    }
}
