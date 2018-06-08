package fr.free.nrw.commons.quiz;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import fr.free.nrw.commons.R;

public class QuizActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.question);
        ImageView imageView = (ImageView) linearLayout.findViewById(R.id.question_image);
        TextView textView = (TextView) linearLayout.findViewById(R.id.question_text);
        textView.setText("Which License can be used to donate this image to public domain?");
    }
}
