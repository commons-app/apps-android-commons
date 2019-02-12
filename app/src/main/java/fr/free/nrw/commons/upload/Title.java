package fr.free.nrw.commons.upload;

import android.text.TextUtils;

import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class Title{

    private String titleText;
    private boolean set;

    @Override
    public String toString() {
        return titleText;
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText;

        if (!TextUtils.isEmpty(titleText)) {
            set = true;
        }
    }

    public boolean isSet() {
        return set;
    }

    public void setSet(boolean set) {
        this.set = set;
    }

    public boolean isEmpty() {
        return titleText==null || titleText.isEmpty();
    }
}
