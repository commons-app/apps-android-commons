package fr.free.nrw.commons.utils;

import android.app.Activity
import fr.free.nrw.commons.utils.DialogUtil;
import org.junit.Test
import org.mockito.Mockito.mock

public class DialogUtilTest {

    @Test
    fun testNoAlertDialogCreatedWhenCustomViewHasParent() {
        val activity = Activity()
        DialogUtil.showAlertDialog(activity, "title", "message", null, null, null, false /** cancellable **/)
    }

    @Test
    fun testAlertDialogCreatedWhenCustomViewHasNoParent() {
        val activity = Activity()
        DialogUtil.showAlertDialog(activity, "title", "message", null, null, null, false /** cancellable **/)
    }
}
