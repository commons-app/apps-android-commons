package fr.free.nrw.commons.nearby;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;

import javax.inject.Inject;

import butterknife.BindView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

public class WikiFeedback extends AppCompatActivity {
TextView textView;
@Inject
    MediaWikiApi mediaWikiApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wiki_feedback);
        textView=findViewById(R.id.descText);
        textView.setText(getIntent().getStringExtra("wikidataEntry"));
        /*String text;
        try {
            text=mediaWikiApi.readFeedback();

        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }
}
