package fr.free.nrw.commons.util

import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher

class MyViewAction {
    companion object {
        fun typeTextInChildViewWithId(id: Int, textToBeTyped: String): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): Matcher<View>? {
                    return null
                }

                override fun getDescription(): String {
                    return "Click on a child view with specified id."
                }

                override fun perform(uiController: UiController, view: View) {
                    val v = view.findViewById<View>(id) as EditText
                    v.setText(textToBeTyped)
                }
            }
        }

        fun selectSpinnerItemInChildViewWithId(id: Int, position: Int): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): Matcher<View>? {
                    return null
                }

                override fun getDescription(): String {
                    return "Click on a child view with specified id."
                }

                override fun perform(uiController: UiController, view: View) {
                    val v = view.findViewById<View>(id) as AppCompatSpinner
                    v.setSelection(position)
                }
            }
        }

    }
}