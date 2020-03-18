package fr.free.nrw.commons.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
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
    @BindView(R.id.quiz_positive_answer) RadioButton positiveAnswer;
    @BindView(R.id.quiz_negative_answer) RadioButton negativeAnswer;
    @BindView(R.id.toolbar) Toolbar toolbar;

    private QuizController quizController = new QuizController();
    private ArrayList<QuizQuestion> quiz = new ArrayList<>();
    private int questionIndex = 0;
    private int score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        quizController.initialize(this);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        displayQuestion();
    }

    /**
     * to move to next question and check whether answer is selected or not
     */
    @OnClick(R.id.next_button)
    public void setNextQuestion(){
        if ( questionIndex <= quiz.size() && (positiveAnswer.isChecked() || negativeAnswer.isChecked())) {
            evaluateScore();
        } else if ( !positiveAnswer.isChecked() && !negativeAnswer.isChecked()){
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(getResources().getString(R.string.warning));
            alert.setMessage(getResources().getString(R.string.warning_for_no_answer));
            alert.setPositiveButton(R.string.continue_message, (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = alert.create();
            dialog.show();
        }

    }

    /**
     * to give warning before ending quiz
     */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getResources().getString(R.string.warning));
        alert.setMessage(getResources().getString(R.string.quiz_back_button));
        alert.setPositiveButton(R.string.continue_message, (dialog, which) -> {
            Intent i = new Intent(QuizActivity.this, QuizResultActivity.class);
            dialog.dismiss();
            i.putExtra("QuizResult",score);
            startActivity(i);
        });
        alert.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog dialog = alert.create();
        dialog.show();
    }

    /**
     * to display the question
     */
    public void displayQuestion() {
        quiz = quizController.getQuiz();
        questionText.setText(quiz.get(questionIndex).getQuestion());
        questionTitle.setText(getResources().getString(R.string.question)+quiz.get(questionIndex).getQuestionNumber());
        imageView.setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_error_outline_black_24dp, getTheme()))
                .setProgressBarImage(new ProgressBarDrawable())
                .build());

        imageView.setImageURI(quiz.get(questionIndex).getUrl());
        new RadioGroupHelper(this, R.id.quiz_positive_answer, R.id.quiz_negative_answer);
        positiveAnswer.setChecked(false);
        negativeAnswer.setChecked(false);
    }

    /**
     * to evaluate score and check whether answer is correct or wrong
     */
    public void evaluateScore() {
        if ((quiz.get(questionIndex).isAnswer() && positiveAnswer.isChecked()) ||
                (!quiz.get(questionIndex).isAnswer() && negativeAnswer.isChecked()) ){
            customAlert(getResources().getString(R.string.correct),quiz.get(questionIndex).getAnswerMessage() );
            score++;
        } else {
            customAlert(getResources().getString(R.string.wrong), quiz.get(questionIndex).getAnswerMessage());
        }
    }

    /**
     * to display explanation after each answer, update questionIndex and move to next question
     * @param title
     * @param Message
     */
    public void customAlert(String title, String Message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(title);
        alert.setMessage(Message);
        alert.setPositiveButton(R.string.continue_message, (dialog, which) -> {
            questionIndex++;
            if (questionIndex == quiz.size()) {
                Intent i = new Intent(QuizActivity.this, QuizResultActivity.class);
                dialog.dismiss();
                i.putExtra("QuizResult",score);
                startActivity(i);
            } else {
                displayQuestion();
            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }
}
