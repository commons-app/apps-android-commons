package fr.free.nrw.commons;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import fr.free.nrw.commons.settings.SettingsActivity;
import fr.free.nrw.commons.utils.CommonsAppSharedPref;

import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {
    private CommonsAppSharedPref prefs;
    private Map<String,?> prefValues;

    @Rule
    public ActivityTestRule<SettingsActivity> activityRule =
            new ActivityTestRule<SettingsActivity>(SettingsActivity.class,
                    false /* Initial touch mode */, true /*  launch activity */) {

                @Override
                protected void afterActivityLaunched() {
                    // save preferences
                    prefs = CommonsAppSharedPref.getInstance(getActivity());
                    prefValues = prefs.getAllPreferences();
                }

                @Override
                protected void afterActivityFinished() {
                    // restore preferences
                    for (Map.Entry<String,?> entry: prefValues.entrySet()) {
                        String key = entry.getKey();
                        Object val = entry.getValue();
                        if (val instanceof String) {
                            prefs.putPreferenceString(key, (String)val);
                        } else if (val instanceof Boolean) {
                            prefs.putPreferenceBoolean(key, (Boolean)val);
                        } else if (val instanceof Integer) {
                            prefs.putPreferenceInt(key, (Integer)val);
                        } else {
                            throw new RuntimeException("type not implemented: " + entry);
                        }
                    }
                }
            };

    @Test
    public void oneLicenseIsChecked() {
        // click "License" (the first item)
        Espresso.onData(anything())
                .inAdapterView(findPreferenceList())
                .atPosition(0)
                .perform(ViewActions.click());

        // test the selected item
        Espresso.onView(ViewMatchers.isChecked())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void afterClickingCcby4ItWillStay() {
        // click "License" (the first item)
        Espresso.onData(anything())
                .inAdapterView(findPreferenceList())
                .atPosition(0)
                .perform(ViewActions.click());

        // click "Attribution 4.0"
        Espresso.onView(
                ViewMatchers.withText(R.string.license_name_cc_by_four)
        ).perform(ViewActions.click());

        // click "License" (the first item)
        Espresso.onData(anything())
                .inAdapterView(findPreferenceList())
                .atPosition(0)
                .perform(ViewActions.click());

        // test the value remains "Attribution 4.0"
        Espresso.onView(ViewMatchers.isChecked())
                .check(ViewAssertions.matches(
                        ViewMatchers.withText(R.string.license_name_cc_by_four)
                ));
    }

    private static Matcher<View> findPreferenceList() {
        return allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(android.R.id.content)),
                ViewMatchers.withId(android.R.id.list),
                ViewMatchers.hasFocus()
        );
    }
}
