package fr.free.nrw.commons.nearby;

import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

public class WikiFeedback extends AppCompatActivity {
TextView textView;
private static final String headerString1="Write something about the ";
    private static final String headerString2=" item. It will be publicly visible.";

TextView textHeader;
@Inject
    MediaWikiApi mediaWikiApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wiki_feedback);
        textView=findViewById(R.id.descText);
        textHeader=findViewById(R.id.textHeader);
        textView.setText(getIntent().getStringExtra("wikidataEntry"));
        String place=getIntent().getStringExtra("place");
        place=headerString1+"'"+place+"'"+headerString2;
        textHeader.setText(place);
    }
}
