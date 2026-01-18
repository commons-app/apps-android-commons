package fr.free.nrw.commons.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import fr.free.nrw.commons.R
import timber.log.Timber


object ViewUtil {

    /**
     * Utility function to show short snack bar
     * @param view
     * @param messageResourceId
     */
    @JvmStatic
    fun showShortSnackbar(view: View, messageResourceId: Int) {
        if (view.context == null) {
            return
        }

        ExecutorUtils.uiExecutor().execute {
            try {
                Snackbar.make(view, messageResourceId, Snackbar.LENGTH_SHORT).show()
            } catch (e: IllegalStateException) {
                Timber.e(e.message)
            }
        }
    }

    @JvmStatic
    fun showLongSnackbar(view: View, text: String) {
        if (view.context == null) {
            return
        }

        ExecutorUtils.uiExecutor().execute {
            try {
                val snackbar = Snackbar.make(view, text, Snackbar.LENGTH_SHORT)
                val snackView = snackbar.view
                val snackText: TextView = snackView.findViewById(R.id.snackbar_text)

                snackView.setBackgroundColor(Color.LTGRAY)
                snackText.setTextColor(ContextCompat.getColor(view.context, R.color.primaryColor))
                snackbar.setActionTextColor(Color.RED)

                snackbar.setAction("Dismiss") { snackbar.dismiss() }
                snackbar.show()

            } catch (e: IllegalStateException) {
                Timber.e(e.message)
            }
        }
    }

    @JvmStatic
    fun showLongToast(context: Context, text: String) {
        if (context == null) {
            return
        }

        ExecutorUtils.uiExecutor().execute {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun showLongToast(context: Context, @StringRes stringResourceId: Int) {
        if (context == null) {
            return
        }

        ExecutorUtils.uiExecutor().execute {
            Toast.makeText(context, context.getString(stringResourceId), Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun showShortToast(context: Context, text: String) {
        if (context == null) {
            return
        }

        ExecutorUtils.uiExecutor().execute {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun showShortToast(context: Context?, @StringRes stringResourceId: Int) {
        if (context == null) {
            return
        }

        ExecutorUtils.uiExecutor().execute {
            Toast.makeText(context, context.getString(stringResourceId), Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun isPortrait(context: Context): Boolean {
        val orientation = (context as Activity).windowManager.defaultDisplay
        return orientation.width < orientation.height
    }

    @JvmStatic
    fun hideKeyboard(view: View?) {
        view?.let {
            val manager = it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            it.clearFocus()
            manager?.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    /**
     * A snack bar which has an action button which on click dismisses the snackbar and invokes the
     * listener passed
     */
    @JvmStatic
    fun showDismissibleSnackBar(
        view: View,
        messageResourceId: Int,
        actionButtonResourceId: Int,
        onClickListener: View.OnClickListener
    ) {
        if (view.context == null) {
            return
        }

        ExecutorUtils.uiExecutor().execute {
            val snackbar = Snackbar.make(view, view.context.getString(messageResourceId), Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction(view.context.getString(actionButtonResourceId)) {
                snackbar.dismiss()
                onClickListener.onClick(it)
            }
            snackbar.show()
        }
    }
}
