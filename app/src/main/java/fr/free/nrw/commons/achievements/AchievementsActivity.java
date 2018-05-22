package fr.free.nrw.commons.achievements;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Binds;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 *  activity for sharing feedback on uploaded activity
 */
public class AchievementsActivity extends NavigationBaseActivity {

    private static final double BADGE_IMAGE_WIDTH_RATIO = 0.4;
    private static final double BADGE_IMAGE_HEIGHT_RATIO = 0.3;

    @BindView(R.id.achievement_badge) ImageView imageView;
    @BindView(R.id.achievement_level)
    TextView textView;
    @BindView(R.id.toolbar) android.support.v7.widget.Toolbar toolbar;
    @Inject SessionManager sessionManager;
    @Inject MediaWikiApi mediaWikiApi;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    /**
     * This method helps in the creation Achievement screen and
     * dynamically set the size of imageView
     *
     * @param savedInstanceState Data bundle
     */
    @Override
    @SuppressLint("StringFormatInvalid")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);
        ButterKnife.bind(this);
        /**
         * DisplayMetrics used to fetch the size of the screen
         */
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        /**
         * Used for the setting the size of imageView at runtime
         */
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)
                imageView.getLayoutParams();
        params.height = (int) (height*BADGE_IMAGE_HEIGHT_RATIO);
        params.width = (int) (width*BADGE_IMAGE_WIDTH_RATIO);
        imageView.setImageResource(R.drawable.featured);
        imageView.requestLayout();

        setSupportActionBar(toolbar);
        setUploadCount();
        initDrawer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.share_app_icon) {
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            Bitmap screenShot = getScreenShot(rootView);
            shareScreen(screenShot);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * To take screenshot of the screen and return it in Bitmap format
     * @param view
     * @return
     */
    public static Bitmap getScreenShot(View view) {
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    /**
     * To take bitmap and store it temporary storage and share it
     * @param bitmap
     */
    void shareScreen ( Bitmap bitmap){
        try {
            File file = new File(this.getExternalCacheDir(),"screen.png");
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true, false);
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, "Share image via"));
        } catch (IOException e) {
            //Do Nothing
        }
    }

    private void setUploadCount() {
        compositeDisposable.add(mediaWikiApi
                .getAchievements(sessionManager.getCurrentAccount().name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                  jsonObject -> parseJson(jsonObject)
                ));
    }

    private void parseJson(JSONObject object){
        Log.i("json",object.toString());
    }

}
