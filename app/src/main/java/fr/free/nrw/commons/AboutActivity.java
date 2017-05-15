package fr.free.nrw.commons;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.theme.BaseActivity;

public class AboutActivity extends BaseActivity {
    @BindView(R.id.about_version) TextView versionText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);

        versionText.setText(BuildConfig.VERSION_NAME);
    }

    public static void startYourself(Context context) {
        Intent settingsIntent = new Intent(context, AboutActivity.class);
        context.startActivity(settingsIntent);
    }
}