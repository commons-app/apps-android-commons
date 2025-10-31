package fr.free.nrw.commons.nearby

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import fr.free.nrw.commons.R

/**
 * Base on https://stackoverflow.com/a/40939367/3950497 answer.
 */
class CheckBoxTriStates : AppCompatCheckBox {
    private var state: Int = UNKNOWN
    var callback: Callback? = null

    /**
     * This is the listener set to the super class which is going to be evoke each
     * time the check state has changed.
     */
    private val privateListener = OnCheckedChangeListener { _, _ ->
        when (state) {
            UNKNOWN -> setState(UNCHECKED)
            UNCHECKED -> setState(CHECKED)
            CHECKED -> setState(UNKNOWN)
        }
    }

    /**
     * Holds a reference to the listener set by a client, if any.
     */
    private var clientListener: OnCheckedChangeListener? = null


    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    fun getState(): Int {
        return state
    }

    fun setState(state: Int) {
        if (this.state != state) {
            this.state = state

            if (clientListener != null) {
                clientListener!!.onCheckedChanged(this, isChecked())
            }

            if (NearbyController.currentLocation != null) {
                callback!!.filterByMarkerType(null, state, false, true)
            }
            updateBtn()
        }
    }

    override fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        // we never truly set the listener to the client implementation, instead we only hold
        // a reference to it and evoke it when needed.

        if (privateListener !== listener) {
            clientListener = listener
        }

        // always use our implementation
        super.setOnCheckedChangeListener(privateListener)
    }

    private fun init() {
        state = UNKNOWN
        updateBtn()
    }

    fun addAction() = setOnCheckedChangeListener(privateListener)

    private fun updateBtn() {
        setButtonDrawable(
            when (state) {
                UNCHECKED -> R.drawable.ic_check_box_outline_blank_black_24dp
                CHECKED -> R.drawable.ic_check_box_black_24dp
                else -> R.drawable.ic_indeterminate_check_box_black_24dp
            }
        )
    }

    interface Callback {
        fun filterByMarkerType(
            selectedLabels: List<Label>?,
            state: Int,
            filterForPlaceState: Boolean,
            filterForAllNoneType: Boolean
        )
    }

    companion object {
        const val UNKNOWN: Int = -1
        const val UNCHECKED: Int = 0
        const val CHECKED: Int = 1
    }
}