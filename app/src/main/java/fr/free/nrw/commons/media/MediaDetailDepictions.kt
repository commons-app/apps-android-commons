package fr.free.nrw.commons.media

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.upload.depicts.DepictsFragment
import java.util.Locale

/**
 * By clicking on the edit depictions button, it will send user to depict fragment
 */
internal fun MediaDetailFragment.onDepictionsEditButtonClicked() {
    binding.mediaDetailDepictionContainer.removeAllViews()
    binding.depictionsEditButton.visibility = View.GONE
    val depictsFragment: Fragment = DepictsFragment()
    val bundle = Bundle()
    bundle.putParcelable("Existing_Depicts", media)
    depictsFragment.arguments = bundle
    val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
    transaction.replace(R.id.mediaDetailFrameLayout, depictsFragment)
    transaction.addToBackStack(null)
    transaction.commit()
}

/**
 * Populates media details fragment with depiction list
 * @param idAndCaptions
 */
internal fun MediaDetailFragment.buildDepictionList(idAndCaptions: List<IdAndLabels>) {
    binding.mediaDetailDepictionContainer.removeAllViews()

    // Create a mutable list from the original list
    val mutableIdAndCaptions = idAndCaptions.toMutableList()

    if (mutableIdAndCaptions.isEmpty()) {
        // Create a placeholder IdAndLabels object and add it to the list
        mutableIdAndCaptions.add(
            IdAndLabels(
                id = media?.pageId ?: "", // Use an empty string if media?.pageId is null
                // Create a Map with the language as the key and the message as the value
                labels = mapOf(Locale.getDefault().language to getString(R.string.detail_panel_cats_none)),
            ),
        )
    }

    val locale: String = Locale.getDefault().language
    for (idAndCaption in mutableIdAndCaptions) {
        binding.mediaDetailDepictionContainer.addView(
            buildDepictLabel(
                getDepictionCaption(idAndCaption, locale),
                idAndCaption.id,
                binding.mediaDetailDepictionContainer,
            ),
        )
    }
}

/** Chooses a depiction caption for the requested locale. */
internal fun MediaDetailFragment.getDepictionCaption(
    idAndCaption: IdAndLabels,
    locale: String,
): String? {
    // Check if the Depiction Caption is available in user's locale
    // if not then check for english, else show any available.
    return idAndCaption.labels[locale]
        ?: idAndCaption.labels["en"]
        ?: idAndCaption.labels.values
            .iterator()
            .next()
}

/**
 * Add view to depictions obtained also tapping on depictions should open the url
 */
internal fun MediaDetailFragment.buildDepictLabel(
    depictionName: String?,
    entityId: String,
    depictionContainer: LinearLayout,
): View {
    val item: View =
        LayoutInflater
            .from(context)
            .inflate(R.layout.detail_category_item, depictionContainer, false)
    val textView: TextView = item.findViewById(R.id.mediaDetailCategoryItemText)
    textView.text = depictionName
    item.setOnClickListener {
        val intent =
            Intent(
                context,
                WikidataItemDetailsActivity::class.java,
            )
        intent.putExtra("wikidataItemName", depictionName)
        intent.putExtra("entityId", entityId)
        intent.putExtra("fragment", "MediaDetailFragment")
        requireContext().startActivity(intent)
    }
    return item
}
