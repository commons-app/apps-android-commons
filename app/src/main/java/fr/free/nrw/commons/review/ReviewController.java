package fr.free.nrw.commons.review;

/**
 * Created by root on 19.05.2018.
 */

public class ReviewController implements ReviewActivity.ReviewCallback {
    public static String fileName;

    @Override
    public void onImageRefreshed(String fileName) {
        this.fileName = fileName;
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
    public void oWrongCategoryReported() {

    }

    @Override
    public void onThankSent() {

    }
}
