package fr.free.nrw.commons.quiz;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

public class QuizActivity extends AppCompatActivity {

    @BindView(R.id.question_image) ImageView imageView;
    @BindView(R.id.question_text) TextView questionText;

    private QuizController quizController = new QuizController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        quizController.initialize();
        ButterKnife.bind(this);
        displayQuestion();

    }

    public void displayQuestion(){
        ArrayList<QuizQuestion> quiz = new ArrayList<QuizQuestion>();
        quiz = quizController.getQuiz();
        questionText.setText(quiz.get(0).getQuestion());
    }
}
