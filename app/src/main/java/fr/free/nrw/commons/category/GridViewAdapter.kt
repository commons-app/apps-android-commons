package fr.free.nrw.commons.category

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R


/**
 * This is created to only display UI implementation. Needs to be changed in real implementation
 */
class GridViewAdapter(
    context: Context,
    layoutResourceId: Int,
    private var data: MutableList<Media>?
) : ArrayAdapter<Media>(context, layoutResourceId, data ?: mutableListOf()) {

    /**
     * Adds more items to the list
     * It's triggered on scrolling down in the list
     * @param images
     */
    fun addItems(images: List<Media>) {
        if (data == null) {
            data = mutableListOf()
        }
        data?.addAll(images)
        notifyDataSetChanged()
    }

    /**
     * Checks the first item in the new list with the old list and returns true if they are the same
     * It's triggered on a successful response of the fetch images API.
     * @param images
     */
    fun containsAll(images: List<Media>?): Boolean {
        if (images.isNullOrEmpty()) {
            return false
        }
        if (data.isNullOrEmpty()) {
            data = mutableListOf()
            return false
        }
        val fileName = data?.get(0)?.filename
        val imageName = images[0].filename
        return imageName == fileName
    }

    override fun isEmpty(): Boolean {
        return data.isNullOrEmpty()
    }

    /**
     * Sets up the UI for the category image item
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.layout_category_images,
            parent,
            false
        )

        val item = data?.get(position)
        val imageView = view.findViewById<SimpleDraweeView>(R.id.categoryImageView)
        val fileName = view.findViewById<TextView>(R.id.categoryImageTitle)
        val uploader = view.findViewById<TextView>(R.id.categoryImageAuthor)

        item?.let {
            fileName.text = it.mostRelevantCaption
            setUploaderView(it, uploader)
            imageView.setImageURI(it.thumbUrl)
        }

        return view
    }

    /**
     * @return the Media item at the given position
     */
    override fun getItem(position: Int): Media? {
        return data?.get(position)
    }

    /**
     * Shows author information if it's present
     * @param item
     * @param uploader
     */
    @SuppressLint("StringFormatInvalid")
    private fun setUploaderView(item: Media, uploader: TextView) {
        if (!item.author.isNullOrEmpty()) {
            uploader.visibility = View.VISIBLE
            uploader.text = context.getString(
                R.string.image_uploaded_by,
                item.user
            )
        } else {
            uploader.visibility = View.GONE
        }
    }
}
