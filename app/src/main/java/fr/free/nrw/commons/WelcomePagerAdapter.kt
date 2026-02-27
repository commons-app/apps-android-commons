package fr.free.nrw.commons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.utils.UnderlineUtils.setUnderlinedText
import fr.free.nrw.commons.utils.handleWebUrl

class WelcomePagerAdapter : RecyclerView.Adapter<WelcomePagerAdapter.ViewHolder>() {
    /**
     * Gets total number of layouts
     * @return Number of layouts
     */
    override fun getItemCount(): Int = PAGE_LAYOUTS.size

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(container: ViewGroup, type: Int): ViewHolder {
        val inflater = LayoutInflater.from(container.context)
        val layout = inflater.inflate(PAGE_LAYOUTS[type], container, false)
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        // If final page
        if (position == PAGE_LAYOUTS.size - 1) {
            // Add link to more information
            val moreInfo = holder.itemView.findViewById<TextView>(R.id.welcomeInfo)
            setUnderlinedText(moreInfo, R.string.welcome_help_button_text)
            moreInfo.setOnClickListener {
                handleWebUrl(
                    holder.itemView.context,
                    "https://commons.wikimedia.org/wiki/Help:Contents".toUri()
                )
            }

            // Handle click of finishTutorialButton ("YES!" button) inside layout
            holder.itemView.findViewById<View>(R.id.finishTutorialButton).setOnClickListener {
                view: View? -> (holder.itemView.context as WelcomeActivity).finishTutorial() }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

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
