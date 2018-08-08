package fr.free.nrw.commons.upload;

import java.util.Locale;

import fr.free.nrw.commons.R;

class Language {

    public static int[] languageGroups={R.string.desc_language_Worldwide,
            R.string.desc_language_America,
            R.string.desc_language_Europe,
            R.string.desc_language_Middle_East,
            R.string.desc_language_Africa,
            R.string.desc_language_Asia,
            R.string.desc_language_Pacific };
    public static int[] languageNames={R.array.desc_languages_Worldwide,
            R.array.desc_languages_America,
            R.array.desc_languages_Europe,
            R.array.desc_languages_Middle_East,
            R.array.desc_languages_Africa,
            R.array.desc_languages_Asia,
            R.array.desc_languages_Pacific
    };
    public static int[] languageCodes={R.array.desc_language_codes_Worldwide,
            R.array.desc_language_codes_America,
            R.array.desc_language_codes_Europe,
            R.array.desc_language_codes_Middle_East,
            R.array.desc_language_codes_Africa,
            R.array.desc_language_codes_Asia,
            R.array.desc_language_codes_Pacific
    };

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
