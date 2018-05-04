package fr.free.nrw.commons;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.PreferenceMatchers;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import fr.free.nrw.commons.settings.SettingsActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {
    private SharedPreferences prefs;
    private Map<String,?> prefValues;

    @Rule
    public ActivityTestRule<SettingsActivity> activityRule =
            new ActivityTestRule<SettingsActivity>(SettingsActivity.class,
                    false /* Initial touch mode */, true /*  launch activity */) {

                @Override
                protected void afterActivityLaunched() {
                    // save preferences
                    prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
                    prefValues = prefs.getAll();
                }

                @Override
                protected void afterActivityFinished() {
                    // restore preferences
                    SharedPreferences.Editor editor = prefs.edit();
                    for (Map.Entry<String,?> entry: prefValues.entrySet()) {
                        String key = entry.getKey();
                        Object val = entry.getValue();
                        if (val instanceof String) {
                            editor.putString(key, (String)val);
                        } else if (val instanceof Boolean) {
                            editor.putBoolean(key, (Boolean)val);
                        } else if (val instanceof Integer) {
                            editor.putInt(key, (Integer)val);
                        } else {
                            throw new RuntimeException("type not implemented: " + entry);
                        }
                    }
                    editor.apply();
                }
            };

    @Test
    public void oneLicenseIsChecked() {
        // click "License" (the first item)
        Espresso.onData(PreferenceMatchers.withKey("defaultLicense"))
                .inAdapterView(ViewMatchers.withId(android.R.id.list))
                .atPosition(0)
                .perform(ViewActions.click());

        // test the selected item
        Espresso.onView(ViewMatchers.isChecked())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void afterClickingCcby4ItWillStay() {
        // click "License" (the first item)
        Espresso.onData(PreferenceMatchers.withKey("defaultLicense"))
                .inAdapterView(ViewMatchers.withId(android.R.id.list))
                .atPosition(0)
                .perform(ViewActions.click());

        // click "Attribution 4.0"
        Espresso.onView(
                ViewMatchers.withText(R.string.license_name_cc_by_four)
        ).perform(ViewActions.click());

        // click "License" (the first item)
        Espresso.onData(PreferenceMatchers.withKey("defaultLicense"))
                .inAdapterView(ViewMatchers.withId(android.R.id.list))
                .atPosition(0)
                .perform(ViewActions.click());

        // test the value remains "Attribution 4.0"
        Espresso.onView(ViewMatchers.isChecked())
                .check(ViewAssertions.matches(
                        ViewMatchers.withText(R.string.license_name_cc_by_four)
                ));
    }
}
