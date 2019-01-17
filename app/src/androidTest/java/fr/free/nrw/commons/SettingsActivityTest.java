package fr.free.nrw.commons;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.PreferenceMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.settings.SettingsActivity;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {
    BasicKvStore prefs;

    @Rule
    public ActivityTestRule<SettingsActivity> activityRule =
            new ActivityTestRule<>(SettingsActivity.class, false, true);

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        String storeName = context.getPackageName() + "_preferences";
        prefs = new BasicKvStore(context, storeName);
    }

    @Test
    public void setRecentUploadLimitTo100() {
        // Open "Use external storage" preference
        Espresso.onData(PreferenceMatchers.withKey("uploads"))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        // Try setting it to 100
        Espresso.onView(withId(android.R.id.edit))
                .perform(replaceText("100"));

        // Click "OK"
        Espresso.onView(allOf(withId(android.R.id.button1), withText("OK")))
                .perform(click());

        // Check setting set to 100 in SharedPreferences
        assertEquals(
                100,
                prefs.getInt(Prefs.UPLOADS_SHOWING, 0)
        );

        // Check displaying 100 in summary text
        Espresso.onData(PreferenceMatchers.withKey("uploads"))
                .inAdapterView(withId(android.R.id.list))
                .onChildView(withId(android.R.id.summary))
                .check(matches(withText("100")));
    }
}
