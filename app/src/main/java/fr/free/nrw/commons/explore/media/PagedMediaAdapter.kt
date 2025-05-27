package fr.free.nrw.commons.explore.media

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.utils.MediaAttributionUtil
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.LayoutCategoryImagesBinding
import fr.free.nrw.commons.explore.paging.BaseViewHolder
import fr.free.nrw.commons.explore.paging.inflate
import fr.free.nrw.commons.media.IdAndLabels
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class PagedMediaAdapter(
    private val onImageClicked: (Int) -> Unit,
    private val mediaDataExtractor: MediaDataExtractor,
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
) : PagedListAdapter<Media, SearchImagesViewHolder>(
        object : DiffUtil.ItemCallback<Media>() {
            override fun areItemsTheSame(
                oldItem: Media,
                newItem: Media,
            ) = oldItem.pageId == newItem.pageId

            override fun areContentsTheSame(
                oldItem: Media,
                newItem: Media,
            ) = oldItem.pageId == newItem.pageId
        },
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = SearchImagesViewHolder(
        parent.inflate(R.layout.layout_category_images),
        onImageClicked,
    )

    override fun onBindViewHolder(
        holder: SearchImagesViewHolder,
        position: Int,
    ) {
        val media = getItem(position) ?: return
        holder.bind(media to position)

        if (!media.getAttributedAuthor().isNullOrEmpty()) {
            return
        }

        compositeDisposable.addAll(
            mediaDataExtractor.fetchCreatorIdsAndLabels(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {  idAndLabels ->
                        media.creatorName = MediaAttributionUtil.getCreatorName(idAndLabels);
                        holder.setAuthorText(media)
                    },
                    { t: Throwable? -> Timber.e(t) })
        )
    }
}

class SearchImagesViewHolder(
    containerView: View,
    val onImageClicked: (Int) -> Unit,
) : BaseViewHolder<Pair<Media, Int>>(containerView) {
    val binding = LayoutCategoryImagesBinding.bind(itemView)

    override fun bind(item: Pair<Media, Int>) {
        val media = item.first
        binding.categoryImageView.setOnClickListener { onImageClicked(item.second) }
        binding.categoryImageTitle.text = media.mostRelevantCaption
        binding.categoryImageView.setImageURI(media.thumbUrl)
        setAuthorText(media)
    }

    fun setAuthorText(media: Media) {
        binding.categoryImageAuthor.text = MediaAttributionUtil.getTagLine(media, containerView.context)
    }
}
