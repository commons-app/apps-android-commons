package fr.free.nrw.commons;

import android.os.Bundle;
import android.widget.TextView;

import fr.free.nrw.commons.theme.BaseActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AboutActivity extends BaseActivity {
    @BindView(R.id.about_version) TextView versionText;
    @BindView(R.id.about_uploads_to) TextView uploadsToText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);

        uploadsToText.setText(CommonsApplication.EVENTLOG_WIKI);
        versionText.setText(BuildConfig.VERSION_NAME);
    }
}