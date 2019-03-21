package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import androidx.constraintlayout.widget.ConstraintLayout;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.theme.BaseActivity;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * In this actvity the user can write feedbacks for the place.
 * Alsdo the activity displays feedbacks given by other users fetched using API
 */

public class WikidataFeedback extends BaseActivity {
    TextView textView;
    TextView textHeader;
    private static String place, wikidataQId;
    ProgressBar progressBar;
    ConstraintLayout activityLayout;
    @Inject
    NearbyController nearbyController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wiki_feedback);
        getWikidataFeedback(place, wikidataQId);
        textView = findViewById(R.id.descText);
        textHeader = findViewById(R.id.textHeader);
        progressBar = findViewById(R.id.progress_bar);
        activityLayout = findViewById(R.id.activity_layout);
    }

    /**
     * This functions starts the activity Wikidata feedback activty of the selected place
     * The API returns feedback given by other users*/

    private void getWikidataFeedback(String name, String wikidataQID) {
        Observable.fromCallable(() -> nearbyController.getFeedback(name, wikidataQID))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        Timber.d("line871" + result);
                        updateUi(name, result);
                    } else {
                        Timber.d("result is null");
                        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }, throwable -> {
                    Timber.e(throwable, "Error occurred while loading notifications");
                    throwable.printStackTrace();
                });
    }

    private void updateUi(String place, String feedback) {
        progressBar.setVisibility(View.GONE);
        activityLayout.setVisibility(View.VISIBLE);
        textView.setText(!feedback.equals("") ? feedback : getString(R.string.no_feedback));
        place = getString(R.string.write_feedback_for_wikidata) + "'" + place + "'" + getString(R.string.item_publicly_visible);
        textHeader.setText(place);
    }

    public static void startYourself(Context context, String name, String wikidataQID) {
        Intent wikidataFeedbackActivity = new Intent(context, WikidataFeedback.class);
        context.startActivity(wikidataFeedbackActivity);
        place = name;
        wikidataQId = wikidataQID;
    }
}
