package fr.free.nrw.commons.quiz;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.dinuscxj.progressbar.CircleProgressBar;

import butterknife.BindView;
import fr.free.nrw.commons.R;

public class QuizResultActivity extends AppCompatActivity {
    @BindView(R.id.result_progress_bar) CircleProgressBar resultProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);
        Bundle extras = getIntent().getExtras();

    }

    public void setScore( int score){
        resultProgressBar.setProgress(score/5 * 100);
    }
}
