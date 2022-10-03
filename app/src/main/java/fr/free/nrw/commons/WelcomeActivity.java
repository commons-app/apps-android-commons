package fr.free.nrw.commons;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import fr.free.nrw.commons.databinding.ActivityWelcomeBinding;
import fr.free.nrw.commons.databinding.PopupForCopyrightBinding;
import fr.free.nrw.commons.quiz.QuizActivity;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.ConfigUtils;

public class WelcomeActivity extends BaseActivity {

    private ActivityWelcomeBinding binding;
    private PopupForCopyrightBinding copyrightBinding;

    private final WelcomePagerAdapter adapter = new WelcomePagerAdapter();
    private boolean isQuiz;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;

    /**
     * Initialises exiting fields and dependencies
     *
     * @param savedInstanceState WelcomeActivity bundled data
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        final View view = binding.getRoot();
        setContentView(view);

        if (getIntent() != null) {
            final Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                isQuiz = bundle.getBoolean("isQuiz");
            }
        } else {
            isQuiz = false;
        }

        // Enable skip button if beta flavor
        if (ConfigUtils.isBetaFlavour()) {
            binding.finishTutorialButton.setVisibility(View.VISIBLE);

            dialogBuilder = new AlertDialog.Builder(this);
            copyrightBinding = PopupForCopyrightBinding.inflate(getLayoutInflater());
            final View contactPopupView = copyrightBinding.getRoot();
            dialogBuilder.setView(contactPopupView);
            dialog = dialogBuilder.create();
            dialog.show();

            copyrightBinding.buttonOk.setOnClickListener(v -> dialog.dismiss());
        }

        binding.welcomePager.setAdapter(adapter);
        binding.welcomePagerIndicator.setViewPager(binding.welcomePager);

        binding.finishTutorialButton.setOnClickListener(v -> finishTutorial());

    }

    /**
     * References WelcomePageAdapter to null before the activity is destroyed
     */
    @Override
    public void onDestroy() {
        if (isQuiz) {
            final Intent i = new Intent(this, QuizActivity.class);
            startActivity(i);
        }
        super.onDestroy();
    }

    /**
     * Creates a way to change current activity to WelcomeActivity
     *
     * @param context Activity context
     */
    public static void startYourself(final Context context) {
        final Intent welcomeIntent = new Intent(context, WelcomeActivity.class);
        context.startActivity(welcomeIntent);
    }

    /**
     * Override onBackPressed() to go to previous tutorial 'pages' if not on first page
     */
    @Override
    public void onBackPressed() {
        if (binding.welcomePager.getCurrentItem() != 0) {
            binding.welcomePager.setCurrentItem(binding.welcomePager.getCurrentItem() - 1, true);
        } else {
            if (defaultKvStore.getBoolean("firstrun", true)) {
                finishAffinity();
            } else {
                super.onBackPressed();
            }
        }
    }

    public void finishTutorial() {
        defaultKvStore.putBoolean("firstrun", false);
        finish();
    }
}
