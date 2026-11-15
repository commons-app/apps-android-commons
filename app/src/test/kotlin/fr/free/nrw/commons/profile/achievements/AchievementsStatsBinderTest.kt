package fr.free.nrw.commons.profile.achievements

import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.databinding.FragmentAchievementsBinding
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.profile.ProfileActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class AchievementsStatsBinderTest {
    @Mock
    private lateinit var sessionManager: SessionManager

    private lateinit var activity: ProfileActivity
    private lateinit var fragment: Fragment
    private lateinit var binding: FragmentAchievementsBinding

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(sessionManager.userName).thenReturn(USER_NAME)

        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        fragment = Fragment()
        activity.supportFragmentManager
            .beginTransaction()
            .add(fragment, null)
            .commitNowAllowingStateLoss()
        binding = FragmentAchievementsBinding.inflate(activity.layoutInflater)
    }

    @Test
    fun bindShowsStatisticsAndLevel() {
        val achievements =
            Achievements(
                uniqueUsedImages = 3,
                thanksReceived = 3,
                featuredImages = 0,
                imagesUploaded = 10,
                revertCount = 1,
            )
        val levelInfo = LevelController.LevelInfo.from(10, 3, 90)

        binder(USER_NAME).bind(achievements)

        assertEquals(View.GONE, binding.progressBar.visibility)
        assertEquals(100 * 10 / levelInfo.maxUploadCount, binding.imagesUploadedProgressbar.progress)
        assertEquals("10/${levelInfo.maxUploadCount}", binding.imageUploadedTVCount.text.toString())
        assertEquals(100 * 3 / levelInfo.maxUniqueImages, binding.imagesUsedByWikiProgressBar.progress)
        assertEquals("3/${levelInfo.maxUniqueImages}", binding.imagesUsedCount.text.toString())
        assertEquals(90, binding.imageRevertsProgressbar.progress)
        assertEquals("90%", binding.imageRevertTVCount.text.toString())
        assertEquals(
            activity.getString(R.string.achievements_revert_limit_message) +
                levelInfo.minNonRevertPercentage + "%",
            binding.imagesRevertLimitText.text.toString(),
        )
        assertEquals(
            activity.getString(R.string.level, levelInfo.levelNumber),
            binding.achievementLevel.text.toString(),
        )
        assertEquals(levelInfo.levelNumber.toString(), binding.achievementBadgeText.text.toString())
        assertEquals(View.GONE, binding.featuredImageIcon.visibility)
        assertEquals(
            levelInfo.levelNumber.toString(),
            BasicKvStore(activity, USER_NAME).getString("userAchievementsLevel"),
        )
    }

    @Test
    fun bindWithoutUploadsTellsOwnUserThereAreNoAchievements() {
        binder(USER_NAME).bind(Achievements())

        assertLatestDialogMessage(activity.getString(R.string.you_have_no_achievements_yet))
        assertEquals(activity.getString(R.string.no_image), binding.imagesUsedCount.text.toString())
        assertEquals(activity.getString(R.string.no_image_reverted), binding.imagesRevertedText.text.toString())
        assertEquals(activity.getString(R.string.no_image_uploaded), binding.imagesUploadTextParam.text.toString())
    }

    @Test
    fun bindWithoutUploadsNamesTheOtherUser() {
        binder(OTHER_USER_NAME).bind(Achievements())

        assertLatestDialogMessage(activity.getString(R.string.no_achievements_yet, OTHER_USER_NAME))
    }

    @Test
    fun showBadgesWithCountHidesViewWhenCountIsZero() {
        binding.thanksImageIcon.visibility = View.VISIBLE

        binder(USER_NAME).showBadgesWithCount(view = binding.thanksImageIcon, count = 0)

        assertEquals(View.GONE, binding.thanksImageIcon.visibility)
    }

    @Test
    fun showBadgesWithCountShowsViewOnNextLayout() {
        binding.thanksImageIcon.visibility = View.GONE

        binder(USER_NAME).showBadgesWithCount(view = binding.thanksImageIcon, count = 3)
        binding.thanksImageIcon.viewTreeObserver.dispatchOnGlobalLayout()

        assertEquals(View.VISIBLE, binding.thanksImageIcon.visibility)
    }

    private fun binder(userName: String) = AchievementsStatsBinder(fragment, binding, sessionManager, userName)

    private fun assertLatestDialogMessage(message: String) {
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(message, shadowOf(ShadowAlertDialog.getLatestAlertDialog()).message.toString())
    }

    private companion object {
        const val USER_NAME = "Test"
        const val OTHER_USER_NAME = "Someone else"
    }
}
