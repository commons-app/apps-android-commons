package fr.free.nrw.commons.quiz

import android.app.Activity
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioButton

import java.util.ArrayList

/**
 * Used to group to or more radio buttons to ensure
 * that at a particular time only one of them is selected
 */
class RadioGroupHelper {

    val radioButtons: MutableList<CompoundButton> = ArrayList()

    /**
     * Constructor to group radio buttons
     * @param radios
     */
    constructor(vararg radios: RadioButton) {
        for (rb in radios) {
            add(rb)
        }
    }

    /**
     * Constructor to group radio buttons
     * @param activity
     * @param radiosIDs
     */
    constructor(activity: Activity, vararg radiosIDs: Int) : this(
        *radiosIDs.map { id -> activity.findViewById<RadioButton>(id) }.toTypedArray()
    )

    /**
     * Constructor to group radio buttons
     * @param rootView
     * @param radiosIDs
     */
    constructor(rootView: View, vararg radiosIDs: Int) {
        for (radioButtonID in radiosIDs) {
            add(rootView.findViewById(radioButtonID))
        }
    }

    private fun add(button: CompoundButton) {
        radioButtons.add(button)
        button.setOnClickListener(onClickListener)
    }

    /**
     * listener to ensure only one of the radio button is selected
     */
    private val onClickListener = View.OnClickListener { v ->
        for (rb in radioButtons) {
            if (rb != v) rb.isChecked = false
        }
    }
}
