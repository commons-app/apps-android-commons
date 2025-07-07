package fr.free.nrw.commons

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.databinding.ActivityWelcomeBinding
import fr.free.nrw.commons.databinding.PopupForCopyrightBinding
import fr.free.nrw.commons.quiz.QuizActivity
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour

class WelcomeActivity : BaseActivity() {
    private var binding: ActivityWelcomeBinding? = null
    private var isQuiz = false

    /**
     * Initialises exiting fields and dependencies
     *
     * @param savedInstanceState WelcomeActivity bundled data
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        isQuiz = intent?.extras?.getBoolean("isQuiz", false) ?: false

        // Enable skip button if beta flavor
        if (isBetaFlavour) {
            binding!!.finishTutorialButton.visibility = View.VISIBLE

            val copyrightBinding = PopupForCopyrightBinding.inflate(layoutInflater)

            val dialog = AlertDialog.Builder(this)
                .setView(copyrightBinding.root)
                .setCancelable(false)
                .create()
            dialog.show()

            copyrightBinding.buttonOk.setOnClickListener { v: View? -> dialog.dismiss() }
        }

        val adapter = WelcomePagerAdapter()
        binding!!.welcomePager.adapter = adapter
        binding!!.welcomePagerIndicator.setViewPager(binding!!.welcomePager)
        binding!!.finishTutorialButton.setOnClickListener { v: View? -> finishTutorial() }
    }

    public override fun onDestroy() {
        if (isQuiz) {
            startActivity(Intent(this, QuizActivity::class.java))
        }
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (binding!!.welcomePager.currentItem != 0) {
            binding!!.welcomePager.setCurrentItem(binding!!.welcomePager.currentItem - 1, true)
        } else {
            if (defaultKvStore.getBoolean("firstrun", true)) {
                finishAffinity()
            } else {
                super.onBackPressed()
            }
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
