package fr.free.nrw.commons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.viewpager.widget.PagerAdapter
import fr.free.nrw.commons.utils.UnderlineUtils.setUnderlinedText
import fr.free.nrw.commons.utils.handleWebUrl

class WelcomePagerAdapter : PagerAdapter() {
    /**
     * Gets total number of layouts
     * @return Number of layouts
     */
    override fun getCount(): Int = PAGE_LAYOUTS.size

    /**
     * Compares given view with provided object
     * @param view Adapter view
     * @param obj Adapter object
     * @return Equality between view and object
     */
    override fun isViewFromObject(view: View, obj: Any): Boolean = (view === obj)

    /**
     * Provides a way to remove an item from container
     * @param container Adapter view group container
     * @param position Index of item
     * @param obj Adapter object
     */
    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) =
        container.removeView(obj as View)

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(container.context)
        val layout = inflater.inflate(PAGE_LAYOUTS[position], container, false) as ViewGroup

        // If final page
        if (position == PAGE_LAYOUTS.size - 1) {
            // Add link to more information
            val moreInfo = layout.findViewById<TextView>(R.id.welcomeInfo)
            setUnderlinedText(moreInfo, R.string.welcome_help_button_text)
            moreInfo.setOnClickListener {
                handleWebUrl(
                    container.context,
                    "https://commons.wikimedia.org/wiki/Help:Contents".toUri()
                )
            }

            // Handle click of finishTutorialButton ("YES!" button) inside layout
            layout.findViewById<View>(R.id.finishTutorialButton)
                .setOnClickListener { view: View? -> (container.context as WelcomeActivity).finishTutorial() }
        }

        container.addView(layout)
        return layout
    }

    companion object {
        private val PAGE_LAYOUTS = intArrayOf(
            R.layout.welcome_wikipedia,
            R.layout.welcome_do_upload,
            R.layout.welcome_dont_upload,
            R.layout.welcome_image_example,
            R.layout.welcome_final
        )
    }
}
