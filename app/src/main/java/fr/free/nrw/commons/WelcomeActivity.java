package fr.free.nrw.commons;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

import com.viewpagerindicator.CirclePageIndicator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.quiz.QuizActivity;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.ConfigUtils;
import android.app.AlertDialog;
import android.widget.Button;

public class WelcomeActivity extends BaseActivity {

    @BindView(R.id.welcomePager)
    ViewPager pager;
    @BindView(R.id.welcomePagerIndicator)
    CirclePageIndicator indicator;

    private WelcomePagerAdapter adapter = new WelcomePagerAdapter();
    private boolean isQuiz;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    Button okButton;

    /**
     * Initialises exiting fields and dependencies
     *
     * @param savedInstanceState WelcomeActivity bundled data
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        if (getIntent() != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                isQuiz = bundle.getBoolean("isQuiz");
            }
        } else {
            isQuiz = false;
        }

        // Enable skip button if beta flavor
        if (ConfigUtils.isBetaFlavour()) {
            findViewById(R.id.finishTutorialButton).setVisibility(View.VISIBLE);

            dialogBuilder = new AlertDialog.Builder(this);
            final View contactPopupView = getLayoutInflater().inflate(R.layout.popup_for_copyright,null);
            dialogBuilder.setView(contactPopupView);
            dialog = dialogBuilder.create();
            dialog.show();

            okButton = dialog.findViewById(R.id.button_ok);
            okButton.setOnClickListener(view -> dialog.dismiss());
        }

        ButterKnife.bind(this);

        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
    }

    /**
     * References WelcomePageAdapter to null before the activity is destroyed
     */
    @Override
    public void onDestroy() {
        if (isQuiz) {
            Intent i = new Intent(WelcomeActivity.this, QuizActivity.class);
            startActivity(i);
        }
        super.onDestroy();
    }

    /**
     * Creates a way to change current activity to WelcomeActivity
     *
     * @param context Activity context
     */
    public static void startYourself(Context context) {
        Intent welcomeIntent = new Intent(context, WelcomeActivity.class);
        context.startActivity(welcomeIntent);
    }

    /**
     * Override onBackPressed() to go to previous tutorial 'pages' if not on first page
     */
    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() != 0) {
            pager.setCurrentItem(pager.getCurrentItem() - 1, true);
        } else {
            if (defaultKvStore.getBoolean("firstrun", true)) {
                finishAffinity();
            } else {
                super.onBackPressed();
            }
        }
    }

    @OnClick(R.id.finishTutorialButton)
    public void finishTutorial() {
        defaultKvStore.putBoolean("firstrun", false);
        finish();
    }
}
