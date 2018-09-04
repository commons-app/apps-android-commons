package fr.free.nrw.commons.upload;

import java.util.Locale;

class Language {

    private Locale locale;
    private boolean isSet = false;

    public Language(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setSet(boolean set) {
        isSet = set;
    }
}
