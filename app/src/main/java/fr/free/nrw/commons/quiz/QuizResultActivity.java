package fr.free.nrw.commons.quiz;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.dinuscxj.progressbar.CircleProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
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
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);
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
}
