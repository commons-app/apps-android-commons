package fr.free.nrw.commons.quiz

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder

import fr.free.nrw.commons.databinding.ActivityQuizBinding
import java.util.ArrayList

import fr.free.nrw.commons.R


class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding
    private val quizController = QuizController()
    private var quiz = ArrayList<QuizQuestion>()
    private var questionIndex = 0
    private var score = 0

    /**
     * isPositiveAnswerChecked : represents yes click event
     */
    private var isPositiveAnswerChecked = false

    /**
     * isNegativeAnswerChecked : represents no click event
     */
    private var isNegativeAnswerChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quizController.initialize(this)
        setSupportActionBar(binding.toolbar.toolbar)
        binding.nextButton.setOnClickListener { notKnowAnswer() }
        displayQuestion()
    }

    /**
     * To move to next question and check whether answer is selected or not
     */
    fun setNextQuestion() {
        if (questionIndex <= quiz.size && (isPositiveAnswerChecked || isNegativeAnswerChecked)) {
            evaluateScore()
        }
    }

    private fun notKnowAnswer() {
        customAlert("Information", quiz[questionIndex].answerMessage)
    }

    /**
     * To give warning before ending quiz
     */
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.warning))
            .setMessage(getString(R.string.quiz_back_button))
            .setCancelable(false)
            .setPositiveButton(R.string.continue_message) { dialog, _ ->
                val intent = Intent(this, QuizResultActivity::class.java)
                dialog.dismiss()
                intent.putExtra("QuizResult", score)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()
            .show()
    }

    /**
     * To display the question
     */
    @SuppressLint("SetTextI18n")
    private fun displayQuestion() {
        quiz = quizController.getQuiz()
        binding.question.questionText.text = quiz[questionIndex].question
        binding.questionTitle.text = getString(R.string.question) + quiz[questionIndex].questionNumber

        binding.question.questionImage.hierarchy = GenericDraweeHierarchyBuilder
            .newInstance(resources)
            .setFailureImage(VectorDrawableCompat.create(resources, R.drawable.ic_error_outline_black_24dp, theme))
            .setProgressBarImage(ProgressBarDrawable())
            .build()

        binding.question.questionImage.setImageURI(quiz[questionIndex].getUrl())
        isPositiveAnswerChecked = false
        isNegativeAnswerChecked = false

        binding.answer.quizPositiveAnswer.setOnClickListener {
            isPositiveAnswerChecked = true
            setNextQuestion()
        }
        binding.answer.quizNegativeAnswer.setOnClickListener {
            isNegativeAnswerChecked = true
            setNextQuestion()
        }
    }

    /**
     * To evaluate score and check whether answer is correct or wrong
     */
    fun evaluateScore() {
        if (
            (quiz[questionIndex].isAnswer && isPositiveAnswerChecked)
                ||
            (!quiz[questionIndex].isAnswer && isNegativeAnswerChecked)
            ) {
            customAlert(
                getString(R.string.correct),
                quiz[questionIndex].answerMessage
            )
            score++
        } else {
            customAlert(
                getString(R.string.wrong),
                quiz[questionIndex].answerMessage
            )
        }
    }

    /**
     * To display explanation after each answer, update questionIndex and move to next question
     * @param title The alert title
     * @param message The alert message
     */
    fun customAlert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.continue_message) { dialog, _ ->
                questionIndex++
                if (questionIndex == quiz.size) {
                    val intent = Intent(this, QuizResultActivity::class.java)
                    dialog.dismiss()
                    intent.putExtra("QuizResult", score)
                    startActivity(intent)
                } else {
                    displayQuestion()
                }
            }
            .create()
            .show()
    }
}
