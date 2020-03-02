package fr.free.nrw.commons.quiz;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dinuscxj.progressbar.CircleProgressBar;

import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;


/**
 *  Displays the final score of quiz and congratulates the user
 */
public class QuizResultActivity extends AppCompatActivity {

    @BindView(R.id.result_progress_bar) CircleProgressBar resultProgressBar;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.congratulatory_message) TextView congratulatoryMessageText;

    private final int NUMBER_OF_QUESTIONS = 5;
    private final int MULTIPLIER_TO_GET_PERCENTAGE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        if ( getIntent() != null) {
            Bundle extras = getIntent().getExtras();
            int score = extras.getInt("QuizResult");
            setScore(score);
        }else{
            startActivityWithFlags(
                    this, MainActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            super.onBackPressed();
        }
    }

    /**
     * to calculate and display percentage and score
     * @param score
     */
    public void setScore(int score) {
        int per = score * MULTIPLIER_TO_GET_PERCENTAGE;
        resultProgressBar.setProgress(per);
        resultProgressBar.setProgressTextFormatPattern(score +" / " + NUMBER_OF_QUESTIONS);
        String message = getResources().getString(R.string.congratulatory_message_quiz,per + "%");
        congratulatoryMessageText.setText(message);
    }

    /**
     * to go to Contibutions Activity
     */
    @OnClick(R.id.quiz_result_next)
    public void launchContributionActivity(){
        startActivityWithFlags(
                this, MainActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    public void onBackPressed() {
        startActivityWithFlags(
                this, MainActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        super.onBackPressed();
    }

    /**
     * Function to call intent to an activity
     * @param context
     * @param cls
     * @param flags
     * @param <T>
     */
    public static <T> void startActivityWithFlags(Context context, Class<T> cls, int... flags) {
        Intent intent = new Intent(context, cls);
        for (int flag: flags) {
            intent.addFlags(flag);
        }
        context.startActivity(intent);
    }

    /**
     * to inflate menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    /**
     * if share option selected then take screenshot and launch alert
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.share_app_icon) {
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            Bitmap screenShot = getScreenShot(rootView);
            showAlert(screenShot);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * to store the screenshot of image in bitmap variable temporarily
     * @param view
     * @return
     */
    public static Bitmap getScreenShot(View view) {
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    /**
     * share the screenshot through social media
     * @param bitmap
     */
    void shareScreen(Bitmap bitmap) {
        try {
            File file = new File(this.getExternalCacheDir(),"screen.png");
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true, false);
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, getString(R.string.share_image_via)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * It display the alertDialog with Image of screenshot
     * @param screenshot
     */
    public void showAlert(Bitmap screenshot) {
        AlertDialog.Builder alertadd = new AlertDialog.Builder(QuizResultActivity.this);
        LayoutInflater factory = LayoutInflater.from(QuizResultActivity.this);
        final View view = factory.inflate(R.layout.image_alert_layout, null);
        ImageView screenShotImage = view.findViewById(R.id.alert_image);
        screenShotImage.setImageBitmap(screenshot);
        TextView shareMessage = view.findViewById(R.id.alert_text);
        shareMessage.setText(R.string.quiz_result_share_message);
        alertadd.setView(view);
        alertadd.setPositiveButton(R.string.about_translate_proceed, (dialog, which) -> shareScreen(screenshot));
        alertadd.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        alertadd.show();
    }
}
