package fr.free.nrw.commons;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toolbar;

import fr.free.nrw.commons.theme.NavigationBaseActivity;

public class Achievements extends AppCompatActivity {

    private static final double badge_image_ratio_width = 0.5;
    private static final double badge_image_ratio_height = 0.5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        ImageView imageView = (ImageView)findViewById(R.id.achievement_badge);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)
                imageView.getLayoutParams();
        params.height = (int) (height*badge_image_ratio_height);
        params.width = (int) (width*badge_image_ratio_width);
        imageView.setImageResource(R.drawable.sydney_opera_house);
        imageView.requestLayout();

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_about);

    }

    


}
