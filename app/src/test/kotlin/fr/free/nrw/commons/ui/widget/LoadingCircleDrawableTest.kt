package fr.free.nrw.commons.ui.widget

import android.animation.ValueAnimator
import android.graphics.drawable.Animatable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestCommonsApplication::class)
class LoadingCircleDrawableTest {
    private fun setAnimationScale(scale: Float) {
        ValueAnimator::class.java
            .getMethod("setDurationScale", Float::class.javaPrimitiveType)
            .invoke(null, scale)
    }

    @After
    fun turnAnimationsBackOn() {
        setAnimationScale(1f)
    }

    private fun loadingCircle(
        theme: Int,
        style: Int = android.R.attr.progressBarStyle,
    ): LoadingCircleDrawable {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), theme)
        val loadingCircle = ProgressBar(context, null, style)
        // Android's own loading circle would be it refresh-arrow drawable here instead
        assertTrue(loadingCircle.indeterminateDrawable is LoadingCircleDrawable)
        return loadingCircle.indeterminateDrawable as LoadingCircleDrawable
    }

    @Test
    fun showsStillRingWhenAnimationsAreOff() {
        setAnimationScale(0f)

        val drawable = loadingCircle(R.style.LightAppTheme)
        drawable.start()

        assertTrue(drawable.isShowingStillRing())
    }

    @Test
    fun showsSpinningCircleWhenAnimationsAreOn() {
        val drawable = loadingCircle(R.style.LightAppTheme)
        drawable.start()

        assertFalse(drawable.isShowingStillRing())
        assertTrue(drawable.drawable is Animatable)
    }

    @Test
    fun darkThemeAndSmallCirclesUseItToo() {
        setAnimationScale(0f)

        val dark = loadingCircle(R.style.DarkAppTheme)
        val small = loadingCircle(R.style.LightAppTheme, android.R.attr.progressBarStyleSmall)
        dark.start()
        small.start()

        assertTrue(dark.isShowingStillRing())
        assertTrue(small.isShowingStillRing())
    }

    @Test
    fun everyLoadingCircleInTheAppUsesIt() {
        // Every layout with a loading circle, and how many it has, so a new one that skips the
        // theme is noticed. Counts include circles from included layouts and video players.
        val expectedCircles =
            mapOf(
                R.layout.activity_notification to 1,
                R.layout.activity_review to 1,
                R.layout.activity_wikidata_feedback to 1,
                R.layout.activity_zoomable to 1,
                R.layout.bottom_sheet_details to 1,
                R.layout.fragment_achievements to 1,
                R.layout.fragment_bookmarks_items to 1,
                R.layout.fragment_bookmarks_locations to 1,
                R.layout.fragment_bookmarks_pictures to 1,
                R.layout.fragment_contributions_list to 1,
                R.layout.fragment_custom_selector to 2,
                R.layout.fragment_explore_map to 1,
                R.layout.fragment_leaderboard to 1,
                // 4 of its own, 1 from show_captions_descriptions, 1 in each of its 2 video players
                R.layout.fragment_media_detail to 7,
                R.layout.fragment_media_detail_pager to 1,
                // 1 of its own, 1 from bottom_sheet_details
                R.layout.fragment_nearby_parent to 2,
                R.layout.fragment_search_paginated to 1,
                R.layout.list_item_progress to 1,
                R.layout.nearby_card_view to 1,
                R.layout.progress_dialog to 1,
                R.layout.show_captions_descriptions to 1,
                R.layout.upload_categories_fragment to 1,
                R.layout.upload_depicts_fragment to 1,
            )
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.LightAppTheme)
        // Many of these layouts show pictures with Fresco, which must be started first
        SoLoader.setInTestMode()
        Fresco.initialize(context)
        val inflater = LayoutInflater.from(context)

        val notFixed = mutableListOf<String>()
        val foundCircles = mutableMapOf<String, Int>()
        expectedCircles.keys.forEach { layout ->
            val name = context.resources.getResourceEntryName(layout)
            val root = inflater.inflate(layout, FrameLayout(context), false)
            val circles = progressBarsIn(root).filter { it.isIndeterminate }
            foundCircles[name] = circles.size
            if (circles.any { it.indeterminateDrawable !is LoadingCircleDrawable }) {
                notFixed += name
            }
        }

        assertEquals("Loading circles not using LoadingCircleDrawable", emptyList<String>(), notFixed)
        assertEquals(
            expectedCircles.mapKeys { context.resources.getResourceEntryName(it.key) },
            foundCircles,
        )
    }

    private fun progressBarsIn(view: View): List<ProgressBar> =
        when (view) {
            is ProgressBar -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { progressBarsIn(view.getChildAt(it)) }
            else -> emptyList()
        }

    @Test
    fun switchesWhenAnimationsAreTurnedOffWhileTheAppIsOpen() {
        val drawable = loadingCircle(R.style.LightAppTheme)
        drawable.start()
        assertFalse(drawable.isShowingStillRing())

        // For example Battery Saver turns on while the loading circle is on screen
        setAnimationScale(0f)
        drawable.stop()
        drawable.start()

        assertTrue(drawable.isShowingStillRing())
    }
}
