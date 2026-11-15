package fr.free.nrw.commons.util

import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher

class MyViewAction {
    companion object {
        fun typeTextInChildViewWithId(
            id: Int,
            textToBeTyped: String,
        ): ViewAction =
            object : ViewAction {
                override fun getConstraints(): Matcher<View>? = null

                override fun getDescription(): String = "Click on a child view with specified id."

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val v = view.findViewById<View>(id) as EditText
                    v.setText(textToBeTyped)
                }
            }

        fun selectSpinnerItemInChildViewWithId(
            id: Int,
            position: Int,
        ): ViewAction =
            object : ViewAction {
                override fun getConstraints(): Matcher<View>? = null

                override fun getDescription(): String = "Click on a child view with specified id."

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val v = view.findViewById<View>(id) as AppCompatSpinner
                    v.setSelection(position)
                }
            }

        fun clickItemWithId(
            id: Int,
            position: Int,
        ): ViewAction =
            object : ViewAction {
                override fun getConstraints(): Matcher<View>? = null

                override fun getDescription(): String = "Click on a child view with specified id."

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val v = view.findViewById<View>(id) as View
                    v.performClick()
                }
            }
    }
}
