package fr.free.nrw.commons.quiz;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;

import fr.free.nrw.commons.databinding.ActivityQuizBinding;
import java.util.ArrayList;

import fr.free.nrw.commons.R;

public class QuizActivity extends AppCompatActivity {

    private ActivityQuizBinding binding;
    private final QuizController quizController = new QuizController();
    private ArrayList<QuizQuestion> quiz = new ArrayList<>();
    private int questionIndex = 0;
    private int score;
    /**
     * isPositiveAnswerChecked : represents yes click event
     */
    private boolean isPositiveAnswerChecked;
    /**
     * isNegativeAnswerChecked : represents no click event
     */
    private boolean isNegativeAnswerChecked;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        quizController.initialize(this);
        setSupportActionBar(binding.toolbar.toolbar);
        binding.nextButton.setOnClickListener(view -> notKnowAnswer());
        displayQuestion();
    }

    /**
     * to move to next question and check whether answer is selected or not
     */
    public void setNextQuestion(){
        if ( questionIndex <= quiz.size() && (isPositiveAnswerChecked || isNegativeAnswerChecked)) {
            evaluateScore();
        }
    }

    public void notKnowAnswer(){
        customAlert("Information", quiz.get(questionIndex).getAnswerMessage());
    }

    /**
     * to give warning before ending quiz
     */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle(getResources().getString(R.string.warning))
            .setMessage(getResources().getString(R.string.quiz_back_button))
            .setPositiveButton(R.string.continue_message, (dialog, which) -> {
                final Intent intent = new Intent(this, QuizResultActivity.class);
                dialog.dismiss();
                intent.putExtra("QuizResult", score);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
            .create()
            .show();
    }

    /**
     * to display the question
     */
    public void displayQuestion() {
        quiz = quizController.getQuiz();
        binding.question.questionText.setText(quiz.get(questionIndex).getQuestion());
        binding.questionTitle.setText(
            getResources().getString(R.string.question) +
                quiz.get(questionIndex).getQuestionNumber()
        );
        binding.question.questionImage.setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_error_outline_black_24dp, getTheme()))
                .setProgressBarImage(new ProgressBarDrawable())
                .build());

        binding.question.questionImage.setImageURI(quiz.get(questionIndex).getUrl());
        isPositiveAnswerChecked = false;
        isNegativeAnswerChecked = false;
        binding.answer.quizPositiveAnswer.setOnClickListener(view -> {
            isPositiveAnswerChecked = true;
            setNextQuestion();
        });
        binding.answer.quizNegativeAnswer.setOnClickListener(view -> {
            isNegativeAnswerChecked = true;
            setNextQuestion();
        });
    }

    /**
     * to evaluate score and check whether answer is correct or wrong
     */
    public void evaluateScore() {
        if ((quiz.get(questionIndex).isAnswer() && isPositiveAnswerChecked) ||
                (!quiz.get(questionIndex).isAnswer() && isNegativeAnswerChecked) ){
            customAlert(getResources().getString(R.string.correct),
                quiz.get(questionIndex).getAnswerMessage());
            score++;
        } else {
            customAlert(getResources().getString(R.string.wrong),
                quiz.get(questionIndex).getAnswerMessage());
        }
    }

    /**
     * to display explanation after each answer, update questionIndex and move to next question
     * @param title the alert title
     * @param Message the alert message
     */
    public void customAlert(final String title, final String Message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(Message)
            .setPositiveButton(R.string.continue_message, (dialog, which) -> {
                questionIndex++;
                if (questionIndex == quiz.size()) {
                    final Intent intent = new Intent(this, QuizResultActivity.class);
                    dialog.dismiss();
                    intent.putExtra("QuizResult", score);
                    startActivity(intent);
                } else {
                    displayQuestion();
                }
            })
            .create()
            .show();
    }
}
