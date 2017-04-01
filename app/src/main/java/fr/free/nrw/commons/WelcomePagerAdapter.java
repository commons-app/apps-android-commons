package fr.free.nrw.commons;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class WelcomePagerAdapter extends PagerAdapter {

    private Context context;

    private static final int PAGE_FINAL = 4;

    static final int[] PAGE_LAYOUTS = new int[]{
            R.layout.welcome_wikipedia,
            R.layout.welcome_do_upload,
            R.layout.welcome_dont_upload,
            R.layout.welcome_image_details,
            R.layout.welcome_final
    };

    public WelcomePagerAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return PAGE_LAYOUTS.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return (view == object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup layout = (ViewGroup) inflater.inflate(PAGE_LAYOUTS[position], container, false);

        if (position == PAGE_FINAL) {
            ViewHolder holder = new ViewHolder(layout, context);
            layout.setTag(holder);
        }
        container.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object obj) {
        container.removeView((View) obj);
    }

    public static class ViewHolder {
        private Context context;

        public ViewHolder(View view, Context context) {
            ButterKnife.bind(this, view);
            this.context = context;
        }

        @OnClick(R.id.welcomeYesButton)
        void onClicked() {
            ((Activity)context).finish();
        }
    }
}
