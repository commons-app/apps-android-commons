package fr.free.nrw.commons.upload;

import android.text.TextUtils;

import timber.log.Timber;

class Title {

    private String titleText;
    private boolean set;

    @Override
    public String toString() {
        return titleText;
    }

    public void setTitleText(String titleText) {
        Timber.i("Setting title text to "+titleText);
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
