package fr.free.nrw.commons;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.util.StringUtil;

public class WelcomePagerAdapter extends PagerAdapter {
    private static final int[] PAGE_LAYOUTS = new int[]{
            R.layout.welcome_wikipedia,
            R.layout.welcome_do_upload,
            R.layout.welcome_dont_upload,
            R.layout.welcome_image_details,
            R.layout.welcome_final
    };
    private static final int PAGE_FINAL = 4;
    private Callback callback;
    private ViewGroup container;

    /**
     * Changes callback to provided one
     *
     * @param callback New callback
     *                 it can be null.
     */
    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    /**
     * Gets total number of layouts
     * @return Number of layouts
     */
    @Override
    public int getCount() {
        return PAGE_LAYOUTS.length;
    }

    /**
     * Compares given view with provided object
     * @param view Adapter view
     * @param object Adapter object
     * @return Equality between view and object
     */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return (view == object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        this.container = container;
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        ViewGroup layout = (ViewGroup) inflater.inflate(PAGE_LAYOUTS[position], container, false);

        // If final page
        if (position == PAGE_FINAL) {
            // Add link to more information
            TextView moreInfo = layout.findViewById(R.id.welcomeInfo);
            moreInfo.setText(StringUtil.fromHtml(WelcomeActivity.moreInformation));
            moreInfo.setOnClickListener(view -> {
                try {
                    Utils.handleWebUrl(
                            container.getContext(),
                            Uri.parse("https://commons.wikimedia.org/wiki/Help:Contents")
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Handle click of finishTutorialButton ("YES!" button) inside layout
            layout.findViewById(R.id.finishTutorialButton)
                    .setOnClickListener(view -> callback.finishTutorial());
        }

        container.addView(layout);
        return layout;
    }

    /**
     * Provides a way to remove an item from container
     * @param container Adapter view group container
     * @param position Index of item
     * @param obj Adapter object
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object obj) {
        container.removeView((View) obj);
    }

    public interface Callback {
        void finishTutorial();
    }
}
