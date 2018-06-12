package fr.free.nrw.commons.quiz;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;

public class QuizActivity extends AppCompatActivity {

    @BindView(R.id.question_image) SimpleDraweeView imageView;
    @BindView(R.id.question_text) TextView questionText;
    @BindView(R.id.question_title) TextView questionTitle;

    private QuizController quizController = new QuizController();
    private int questionIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        Fresco.initialize(this);

        quizController.initialize(this);
        ButterKnife.bind(this);
        displayQuestion();
        questionIndex++;

    }

    @OnClick(R.id.next_button)
    public void setNextQuestion(){
       if( questionIndex < 5) {
           displayQuestion();
           questionIndex++;
       }
    }

    public void displayQuestion(){
        ArrayList<QuizQuestion> quiz = new ArrayList<QuizQuestion>();
        quiz = quizController.getQuiz();
        questionText.setText(quiz.get(questionIndex).getQuestion());
        questionTitle.setText(getResources().getString(R.string.question)+quiz.get(questionIndex).getQuestionNumber());
        imageView.setImageURI(quiz.get(questionIndex).getUrl());
    }
}
