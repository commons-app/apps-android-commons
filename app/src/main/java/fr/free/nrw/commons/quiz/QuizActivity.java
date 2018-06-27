package fr.free.nrw.commons.quiz;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.R;

public class QuizActivity extends AppCompatActivity {

    @BindView(R.id.question_image) SimpleDraweeView imageView;
    @BindView(R.id.question_text) TextView questionText;
    @BindView(R.id.question_title) TextView questionTitle;
    @BindView(R.id.quiz_positive_answer) RadioButton positiveAnswer;
    @BindView(R.id.quiz_negative_answer) RadioButton negativeAnswer;

    private QuizController quizController = new QuizController();
    private ArrayList<QuizQuestion> quiz = new ArrayList<QuizQuestion>();
    private int questionIndex = 0;
    private int score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        Fresco.initialize(this);

        setTitle(getResources().getString(R.string.quiz));

        quizController.initialize(this);
        ButterKnife.bind(this);
        displayQuestion();
    }

    @OnClick(R.id.next_button)
    public void setNextQuestion(){
       if( questionIndex <= quiz.size() && (positiveAnswer.isChecked() || negativeAnswer.isChecked())) {
           evaluateScore();
       } else if ( !positiveAnswer.isChecked() && !negativeAnswer.isChecked()){
           customAlert(getResources().getString(R.string.warning), getResources().getString(R.string.warning_for_no_answer));
       }

    }

    public void displayQuestion(){
        quiz = quizController.getQuiz();
        questionText.setText(quiz.get(questionIndex).getQuestion());
        questionTitle.setText(getResources().getString(R.string.question)+quiz.get(questionIndex).getQuestionNumber());
        imageView.setImageURI(quiz.get(questionIndex).getUrl());
        RadioGroupHelper group = new RadioGroupHelper(this,R.id.quiz_positive_answer,R.id.quiz_negative_answer);
        positiveAnswer.setChecked(false);
        negativeAnswer.setChecked(false);
    }

    public void evaluateScore(){
        if((quiz.get(questionIndex).isAnswer() && positiveAnswer.isChecked()) ||
                (!quiz.get(questionIndex).isAnswer() && negativeAnswer.isChecked()) ){
            customAlert("Correct",quiz.get(questionIndex).getAnswerMessage() );
            score++;
        } else{
            customAlert("Wrong", quiz.get(questionIndex).getAnswerMessage());
        }
    }

    public void customAlert( String title, String Message){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(title);
        alert.setMessage(Message);
        alert.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                questionIndex++;
                if(questionIndex == quiz.size()){
                    Intent i = new Intent(QuizActivity.this, QuizResultActivity.class);
                    dialog.dismiss();
                    i.putExtra("QuizResult",score);
                    startActivity(i);
                }else {
                    displayQuestion();
                }
            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }
}
