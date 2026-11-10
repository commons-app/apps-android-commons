package fr.free.nrw.commons.media

import android.view.View
import fr.free.nrw.commons.language.AppLanguageLookUpTable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Objects

/** Toggles the multilingual captions and descriptions. */
internal fun MediaDetailFragment.showCaptionAndDescription() {
    if (binding.dummyCaptionDescriptionContainer.visibility == View.GONE) {
        binding.dummyCaptionDescriptionContainer.visibility = View.VISIBLE
        onBackPressedCallback.isEnabled = true
        setUpCaptionAndDescriptionLayout()
    } else {
        binding.dummyCaptionDescriptionContainer.visibility = View.GONE
        onBackPressedCallback.isEnabled = false
    }
}

/**
 * setUp Caption And Description Layout
 */
internal fun MediaDetailFragment.setUpCaptionAndDescriptionLayout() {
    val captions: List<Caption> = captions

    if (descriptionHtmlCode == null) {
        binding.showCaptionsBinding.pbCircular.visibility = View.VISIBLE
    }

    description
    val adapter = CaptionListViewAdapter(captions)
    binding.showCaptionsBinding.captionListview.adapter = adapter
}

internal val MediaDetailFragment.captions: List<Caption>
    /**
     * Generate the caption with language
     */
    get() {
        val captionList: MutableList<Caption> =
            ArrayList()
        val captions: Map<String, String> = media!!.captions
        val appLanguageLookUpTable =
            AppLanguageLookUpTable(requireContext())
        for (map: Map.Entry<String, String> in captions.entries) {
            val language: String? = appLanguageLookUpTable.getLocalizedName(map.key)
            val languageCaption: String = map.value
            captionList.add(Caption(language, languageCaption))
        }

        if (captionList.size == 0) {
            captionList.add(Caption("", "No Caption"))
        }
        return captionList
    }

internal val MediaDetailFragment.description: Unit
    get() {
        viewDisposables.add(
            mediaDataExtractor
                .getHtmlOfPage(
                    Objects.requireNonNull(media?.filename!!),
                ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { s: String -> extractDescription(s) },
                    { t: Throwable? -> Timber.e(t) },
                ),
        )
    }

/**
 * extract the description from html of imagepage
 */
internal fun MediaDetailFragment.extractDescription(s: String) {
    val descriptionClassName = "<td class=\"description\">"
    val start: Int = s.indexOf(descriptionClassName) + descriptionClassName.length
    val end: Int = s.indexOf("</td>", start)
    descriptionHtmlCode = ""
    for (i in start until end) {
        descriptionHtmlCode += s.toCharArray()[i]
    }

    binding.showCaptionsBinding.descriptionWebview
        .loadDataWithBaseURL(null, descriptionHtmlCode!!, "text/html", "utf-8", null)
    binding.showCaptionsBinding.pbCircular.visibility = View.GONE
}
