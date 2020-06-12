package fr.free.nrw.commons.explore.categories

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CATEGORY_PREFIX
import kotlinx.android.synthetic.main.item_recent_searches.*


fun searchCategoryDelegate(onCategoryClicked: (String) -> Unit) =
    adapterDelegateLayoutContainer<String, String>(R.layout.item_recent_searches) {
        containerView.setOnClickListener { onCategoryClicked(item) }
        bind {
            textView1.text = item.substringAfter(CATEGORY_PREFIX)
        }
    }
