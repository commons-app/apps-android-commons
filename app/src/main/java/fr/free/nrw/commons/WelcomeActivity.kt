package fr.free.nrw.commons

import android.content.Context
import android.content.Intent
import android.os.Bundle
import fr.free.nrw.commons.quiz.QuizActivity
import fr.free.nrw.commons.theme.BaseActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour

class WelcomeActivity : BaseActivity() {
    private var isQuiz = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isQuiz = intent?.extras?.getBoolean("isQuiz", false) ?: false

        setContent {
            MaterialTheme {
                WelcomeScreen(
                    isBetaFlavour = isBetaFlavour,
                    pageCount = 4,
                    onSkipClicked = { finishTutorial() },
                    onBackPressedAtStart = { handleBackAtStart() }
                )
            }
        }
    }

    public override fun onDestroy() {
        if (isQuiz) {
            startActivity(Intent(this, QuizActivity::class.java))
        }
        super.onDestroy()
    }

    private fun handleBackAtStart() {
        if (defaultKvStore.getBoolean("firstrun", true)) {
            finishAffinity()
        } else {
            finish()
        }
    }

    fun finishTutorial() {
        defaultKvStore.putBoolean("firstrun", false)
        finish()
    }
}

fun Context.startWelcome() {
    startActivity(Intent(this, WelcomeActivity::class.java))
}
