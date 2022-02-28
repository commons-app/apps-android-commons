package fr.free.nrw.commons.feedback

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.FragmentTransaction
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class FeedbackDialogTests {
    @Mock
    var button: Button? = null
    @Mock
    var apiLevel: CheckBox? = null
    @Mock
    var androidVersion: CheckBox? = null
    @Mock
    var deviceManufacturer: CheckBox? = null
    @Mock
    var deviceModel: CheckBox? = null
    @Mock
    var deviceName: CheckBox? = null
    @Mock
    var networkType: CheckBox? = null
    @Mock
    var userName: CheckBox? = null
    @Mock
    var feedbackDescription: PasteSensitiveTextInputEditText? = null

    @Mock
    var feedbackController: FeedbackController? = null

    @Mock
    private val onFeedbackSubmitCallback: OnFeedbackSubmitCallback? = null
    private lateinit var dialog: FeedbackDialog

    private lateinit var context: Context

//    @Before
//    fun setUp() {
//        MockitoAnnotations.initMocks(this)
//
//        context = RuntimeEnvironment.application.applicationContext
//        AppAdapter.set(TestAppAdapter())
//
//        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
//
////        dialog = new FeedbackDialog()
////        dialog.show()
//
//        tvTitle = view.findViewById(R.id.tv_title)
//        tooltip = view.findViewById(R.id.tooltip)
//        rvDescriptions = view.findViewById(R.id.rv_descriptions)
//        btnPrevious = view.findViewById(R.id.btn_previous)
//        btnNext = view.findViewById(R.id.btn_next)
//        btnCopyToSubsequentMedia = view.findViewById(R.id.btn_copy_subsequent_media)
//        photoViewBackgroundImage = view.findViewById(R.id.backgroundImage)
//        ibMap = view.findViewById(R.id.ib_map)
//        llContainerMediaDetail = view.findViewById(R.id.ll_container_media_detail)
//        ibExpandCollapse = view.findViewById(R.id.ib_expand_collapse)
//
//        Whitebox.setInternalState(fragment, "tvTitle", tvTitle)
//        Whitebox.setInternalState(fragment, "tooltip", tooltip)
//        Whitebox.setInternalState(fragment, "callback", callback)
//        Whitebox.setInternalState(fragment, "rvDescriptions", rvDescriptions)
//        Whitebox.setInternalState(fragment, "btnPrevious", btnPrevious)
//        Whitebox.setInternalState(fragment, "btnNext", btnNext)
//        Whitebox.setInternalState(fragment, "btnCopyToSubsequentMedia", btnCopyToSubsequentMedia)
//        Whitebox.setInternalState(fragment, "photoViewBackgroundImage", photoViewBackgroundImage)
//        Whitebox.setInternalState(fragment, "uploadMediaDetailAdapter", uploadMediaDetailAdapter)
//        Whitebox.setInternalState(fragment, "ibMap", ibMap)
//        Whitebox.setInternalState(fragment, "llContainerMediaDetail", llContainerMediaDetail)
//        Whitebox.setInternalState(fragment, "ibExpandCollapse", ibExpandCollapse)
//    }

    fun testOnCreate() {

    }

}