package fr.free.nrw.commons;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.viewpagerindicator.CirclePageIndicator;

import fr.free.nrw.commons.theme.BaseActivity;

public class WelcomeActivity extends BaseActivity {
    static final int PAGE_WIKIPEDIA = 0,
            PAGE_DO_UPLOAD = 1,
            PAGE_DONT_UPLOAD = 2,
            PAGE_IMAGE_DETAILS = 3,
            PAGE_FINAL = 4;
    static final int[] pageLayouts = new int[] {
            R.layout.welcome_wikipedia,
            R.layout.welcome_do_upload,
            R.layout.welcome_dont_upload,
            R.layout.welcome_image_details,
            R.layout.welcome_final
    };

    private ViewPager pager;
    private Button yesButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        pager = (ViewPager)findViewById(R.id.welcomePager);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return pageLayouts.length;
            }

            @Override
            public boolean isViewFromObject(View view, Object o) {
                return (view == o);
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View view = getLayoutInflater().inflate(pageLayouts[position], null);
                container.addView(view);
                if (position == PAGE_FINAL) {
                    yesButton = (Button)view.findViewById(R.id.welcomeYesButton);
                    yesButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    });
                }
                return view;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object obj) {
                yesButton = null;
                container.removeView((View)obj);
            }
        });

        CirclePageIndicator indicator = (CirclePageIndicator)findViewById(R.id.welcomePagerIndicator);
        indicator.setViewPager(pager);
    }
}
