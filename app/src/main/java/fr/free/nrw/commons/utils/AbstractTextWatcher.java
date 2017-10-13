package fr.free.nrw.commons.utils;

import android.text.Editable;
import android.text.TextWatcher;

public class AbstractTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // this space intentionally left blank
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // no, really
    }

    @Override
    public void afterTextChanged(Editable s) {
        // I meant it this way!  :-)
    }
}
