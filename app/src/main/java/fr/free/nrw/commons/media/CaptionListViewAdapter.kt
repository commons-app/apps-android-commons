package fr.free.nrw.commons.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import fr.free.nrw.commons.R

/**
 * Adapter for Caption Listview
 */
class CaptionListViewAdapter(var captions: List<Caption>) : BaseAdapter() {
    override fun getCount(): Int {
        return captions.size
    }

    override fun getItem(i: Int): Any? {
        return null
    }

    override fun getItemId(i: Int): Long {
        return 0
    }

    override fun getView(i: Int, view: View, viewGroup: ViewGroup): View {
        val captionLanguageTextView: TextView
        val captionTextView: TextView
        val captionLayout = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.caption_item, null)
        captionLanguageTextView = captionLayout.findViewById(R.id.caption_language_textview)
        captionTextView = captionLayout.findViewById(R.id.caption_text)
        if (captions.size == 1 && captions[0].value == "No Caption") {
            captionLanguageTextView.text = captions[i].language
            captionTextView.text = captions[i].value
        } else {
            captionLanguageTextView.text = captions[i].language + ":"
            captionTextView.text = captions[i].value
        }

        return captionLayout
    }
}
