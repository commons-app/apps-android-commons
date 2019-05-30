package fr.free.nrw.commons;

import android.net.Uri;
import android.text.Html;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.viewpager.widget.PagerAdapter;
import org.wikipedia.util.StringUtil;

public class WelcomePagerAdapter extends PagerAdapter {
    private static final int[] PAGE_LAYOUTS = new int[]{
            R.layout.welcome_wikipedia,
            R.layout.welcome_do_upload,
            R.layout.welcome_dont_upload,
            R.layout.welcome_image_example,
            R.layout.welcome_final
    };

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
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        ViewGroup layout = (ViewGroup) inflater.inflate(PAGE_LAYOUTS[position], container, false);

        // If final page
        if (position == PAGE_LAYOUTS.length - 1) {
            // Add link to more information
            TextView moreInfo = layout.findViewById(R.id.welcomeInfo);
            moreInfo.setText(Html.fromHtml(container.getContext().getString(R.string.welcome_help_button_text)));
            moreInfo.setOnClickListener(view -> Utils.handleWebUrl(
                    container.getContext(),
                    Uri.parse("https://commons.wikimedia.org/wiki/Help:Contents")
            ));

            // Handle click of finishTutorialButton ("YES!" button) inside layout
            layout.findViewById(R.id.finishTutorialButton)
                    .setOnClickListener(view -> ((WelcomeActivity) container.getContext()).finishTutorial());
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
}
