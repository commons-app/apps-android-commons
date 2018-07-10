package fr.free.nrw.commons.quiz;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.dinuscxj.progressbar.CircleProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsActivity;

import android.support.v7.widget.Toolbar;
import android.widget.TextView;

public class QuizResultActivity extends AppCompatActivity {
    @BindView(R.id.result_progress_bar)
    CircleProgressBar resultProgressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.congratulatory_message)
    TextView congratulatoryMessageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        int score = extras.getInt("QuizResult");
        setScore(score);

    }

    public void setScore( int score){
        int per = score * 20;
        Log.i("percentage", "setScore: " + per);
        resultProgressBar.setProgress(per);
        resultProgressBar.setProgressTextFormatPattern(score +" / " + 5);
        String message = getResources().getString(R.string.congratulatory_message_quiz,per + "%");
        congratulatoryMessageText.setText(message);
    }

    /**
     * to go to Contibutions Activity
     */
    @OnClick(R.id.quiz_result_next)
    public void launchContributionActivity(){
        startActivityWithFlags(
                this, ContributionsActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    public void onBackPressed() {
        startActivityWithFlags(
                this, ContributionsActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
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
}
