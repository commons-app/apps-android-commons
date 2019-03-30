package fr.free.nrw.commons

import android.content.res.Configuration
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.contributions.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.R.attr.orientation



@MediumTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun orientationChange(){
        val firstConfiguration = Configuration()
        val nextConfiguration = getOtherConfiguration()
        activityRule.activity.onConfigurationChanged(nextConfiguration)
        assert(firstConfiguration != nextConfiguration)
    }

    private fun getOtherConfiguration(): Configuration {
        val configuration = Configuration()
        configuration.orientation = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            Configuration.ORIENTATION_PORTRAIT
        else
            Configuration.ORIENTATION_LANDSCAPE
        return configuration
    }
}