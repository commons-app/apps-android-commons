package fr.free.nrw.commons.quiz

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import fr.free.nrw.commons.databinding.ActivityQuizResultBinding
import java.io.File
import java.io.FileOutputStream

import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity


/**
 * Displays the final score of quiz and congratulates the user
 */
class QuizResultActivity : AppCompatActivity() {

    private var binding: ActivityQuizResultBinding? = null
    private val numberOfQuestions = 5
    private val multiplierToGetPercentage = 20

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizResultBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbar?.toolbar)

        binding?.quizResultNext?.setOnClickListener {
            launchContributionActivity()
        }

        intent?.extras?.let { extras ->
            val score = extras.getInt("QuizResult", 0)
            setScore(score)
        } ?: run {
            startActivityWithFlags(
                this, MainActivity::class.java,
                Intent.FLAG_ACTIVITY_CLEAR_TOP, Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    /**
     * To calculate and display percentage and score
     * @param score
     */
    @SuppressLint("StringFormatInvalid", "SetTextI18n")
    fun setScore(score: Int) {
        val scorePercent = score * multiplierToGetPercentage
        binding?.resultProgressBar?.progress = scorePercent
        binding?.tvResultProgress?.text = "$score / $numberOfQuestions"
        val message = resources.getString(R.string.congratulatory_message_quiz, "$scorePercent%")
        binding?.congratulatoryMessage?.text = message
    }

    /**
     * To go to Contributions Activity
     */
    fun launchContributionActivity() {
        startActivityWithFlags(
            this, MainActivity::class.java,
            Intent.FLAG_ACTIVITY_CLEAR_TOP, Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
    }

    override fun onBackPressed() {
        startActivityWithFlags(
            this, MainActivity::class.java,
            Intent.FLAG_ACTIVITY_CLEAR_TOP, Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        super.onBackPressed()
    }

    /**
     * Function to call intent to an activity
     * @param context
     * @param cls
     * @param flags
     */
    companion object {
        fun <T> startActivityWithFlags(context: Context, cls: Class<T>, vararg flags: Int) {
            val intent = Intent(context, cls)
            flags.forEach { flag -> intent.addFlags(flag) }
            context.startActivity(intent)
        }
    }

    /**
     * To inflate menu
     * @param menu
     * @return
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_about, menu)
        return true
    }

    /**
     * If share option selected then take screenshot and launch alert
     * @param item
     * @return
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.share_app_icon) {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            val screenShot = getScreenShot(rootView)
            showAlert(screenShot)
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * To store the screenshot of image in bitmap variable temporarily
     * @param view
     * @return
     */
    fun getScreenShot(view: View): Bitmap {
        val screenView = view.rootView
        screenView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(screenView.drawingCache)
        screenView.isDrawingCacheEnabled = false
        return bitmap
    }

    /**
     * Share the screenshot through social media
     * @param bitmap
     */
    @SuppressLint("SetWorldReadable")
    fun shareScreen(bitmap: Bitmap) {
        try {
            val file = File(this.externalCacheDir, "screen.png")
            FileOutputStream(file).use { fOut ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)
                fOut.flush()
            }
            file.setReadable(true, false)
            val intent = Intent(Intent.ACTION_SEND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                type = "image/png"
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_image_via)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * It displays the AlertDialog with Image of screenshot
     * @param screenshot
     */
    fun showAlert(screenshot: Bitmap) {
        val alertadd = AlertDialog.Builder(this)
        val factory = LayoutInflater.from(this)
        val view = factory.inflate(R.layout.image_alert_layout, null)
        val screenShotImage = view.findViewById<ImageView>(R.id.alert_image)
        screenShotImage.setImageBitmap(screenshot)
        val shareMessage = view.findViewById<TextView>(R.id.alert_text)
        shareMessage.setText(R.string.quiz_result_share_message)
        alertadd.setView(view)
        alertadd.setCancelable(false)
        alertadd.setPositiveButton(R.string.about_translate_proceed) { dialog, _ ->
            shareScreen(screenshot)
        }
        alertadd.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        alertadd.show()
    }
}
