package fr.free.nrw.commons.upload;

import android.text.TextUtils;

public class Title{

    private String titleText;
    private boolean set;

    @Override
    public String toString() {
        return titleText;
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText.trim();

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

    public String getTitleText() {
        return titleText;
    }
}
