package fr.free.nrw.commons.quiz;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.dinuscxj.progressbar.CircleProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

public class QuizResultActivity extends AppCompatActivity {
    @BindView(R.id.result_progress_bar) CircleProgressBar resultProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);
        ButterKnife.bind(this);
        Bundle extras = getIntent().getExtras();
        int score = extras.getInt("QuizResult");
        setScore(score);
        Log.i("score", "onCreate: "+score);

    }

    public void setScore( int score){
        int per = score * 20;
        Log.i("percentage", "setScore: " + per);
        resultProgressBar.setProgress(per);
        resultProgressBar.setProgressTextFormatPattern(score +" / " + 5);
    }
}
