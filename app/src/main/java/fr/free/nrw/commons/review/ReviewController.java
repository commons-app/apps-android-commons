package fr.free.nrw.commons.review;

import android.content.Context;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;

/**
 * Created by root on 19.05.2018.
 */

public class ReviewController implements ReviewActivity.ReviewCallback {
    public static String fileName;
    protected static ArrayList<String> categories;
    ReviewPagerAdapter reviewPagerAdapter;
    ViewPager viewPager;

    ReviewController(Context context) {
        reviewPagerAdapter = ((ReviewActivity)context).reviewPagerAdapter;
        viewPager = ((ReviewActivity)context).pager;
    }

    @Override
    public void onImageRefreshed(String fileName) {
        ReviewController.fileName = fileName;
        ReviewController.categories = new ArrayList<>();
    }

    public void onCategoriesRefreshed(ArrayList<String> categories) {
        ReviewController.categories = categories;
    }

    @Override
    public void onQuestionChanged() {

    }

    @Override
    public void onSurveyFinished() {

    }

    @Override
    public void onImproperImageReported() {

    }

    @Override
    public void onLicenceViolationReported() {

    }

    @Override
    public void onWrongCategoryReported() {

    }

    @Override
    public void onThankSent() {

    }
}
