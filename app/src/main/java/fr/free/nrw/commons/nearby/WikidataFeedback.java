package fr.free.nrw.commons.nearby;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import fr.free.nrw.commons.R;

/**
* In this actvity the user can write feedbacks for the place.
 * Alsdo the activity displays feedbacks given by other users fetched using API*/

public class WikidataFeedback extends AppCompatActivity {
    TextView textView;
    TextView textHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wiki_feedback);
        textView = findViewById(R.id.descText);
        textHeader = findViewById(R.id.textHeader);
        textView.setText(getIntent().getStringExtra("wikidataEntry"));
        String place = getIntent().getStringExtra("place");
        place = getString(R.string.write_feedback_for_wikidata) + "'" + place + "'" + getString(R.string.item_publicly_visible);
        textHeader.setText(place);
    }
}
