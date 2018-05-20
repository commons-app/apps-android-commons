package fr.free.nrw.commons.review;

import android.content.Context;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.delete.DeleteTask;
import fr.free.nrw.commons.mwapi.Revision;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by nes on 19.05.2018.
 */

public class ReviewController {
    public static String fileName;
    public static Revision firstRevision; // TODO: maybe we can expand this class to include fileName
    protected static ArrayList<String> categories;
    ReviewPagerAdapter reviewPagerAdapter;
    ViewPager viewPager;
    ReviewActivity reviewActivity;

    ReviewController(Context context) {
        reviewActivity =  (ReviewActivity)context;
        reviewPagerAdapter = reviewActivity.reviewPagerAdapter;
        viewPager = ((ReviewActivity)context).reviewPager;
    }

    public void onImageRefreshed(String fileName) {
        ReviewController.fileName = fileName;
        ReviewController.categories = new ArrayList<>();

        reviewActivity.mwApi.firstRevisionOfFile("File:" + fileName).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(revision -> {
                    ReviewController.firstRevision = revision;
                });
    }

    public void onCategoriesRefreshed(ArrayList<String> categories) {
        ReviewController.categories = categories;
    }

    public void swipeToNext() {
        int nextPos = viewPager.getCurrentItem()+1;
        if (nextPos <= 3) {
            viewPager.setCurrentItem(nextPos);
        } else {
            reviewActivity.runRandomizer();
        }
    }

    public void reportSpam() {
        DeleteTask.askReasonAndExecute(new Media("File:"+fileName),
                reviewActivity,
                reviewActivity.getResources().getString(R.string.review_spam_report_question),
                reviewActivity.getResources().getString(R.string.review_spam_report_default_answer));
    }

    public void reportPossibleCopyRightViolation() {
        DeleteTask.askReasonAndExecute(new Media("File:"+fileName),
                reviewActivity,
                reviewActivity.getResources().getString(R.string.review_c_violation_report_question),
                reviewActivity.getResources().getString(R.string.review_c_violation_report_default_answer));
    }

    public void reportWrongCategory() {
        new CheckCategoryTask(reviewActivity, new Media("File:"+fileName)).execute();
        swipeToNext();
    }

    public void sendThanks() {
        new SendThankTask(reviewActivity, new Media("File:"+fileName), firstRevision).execute();
        swipeToNext();
    }
}
