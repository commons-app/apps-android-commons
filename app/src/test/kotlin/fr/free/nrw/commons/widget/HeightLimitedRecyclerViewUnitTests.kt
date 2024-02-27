package fr.free.nrw.commons.widget

import android.app.Activity
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class HeightLimitedRecyclerViewUnitTests {

    private lateinit var activityController: ActivityController<Activity>
    private lateinit var activity: Activity
    private lateinit var recyclerView: HeightLimitedRecyclerView


    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(Activity::class.java)
        activity = activityController.get()

        recyclerView = HeightLimitedRecyclerView(activity)
        recyclerView = HeightLimitedRecyclerView(activity, null)
        recyclerView = HeightLimitedRecyclerView(activity, null, 0)
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        assertThat(recyclerView, notNullValue())
    }

    @Test
    @Throws(Exception::class)
    fun testOnMeasure() {
        val method: Method = HeightLimitedRecyclerView::class.java.getDeclaredMethod(
            "onMeasure",
            Int::class.java,
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(recyclerView, 0, 0)
    }

}