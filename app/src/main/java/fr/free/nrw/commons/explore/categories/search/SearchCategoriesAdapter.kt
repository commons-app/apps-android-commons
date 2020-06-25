package fr.free.nrw.commons.explore.categories.search

import fr.free.nrw.commons.upload.categories.BaseDelegateAdapter


class SearchCategoriesAdapter(onCateoryClicked: (String) -> Unit) : BaseDelegateAdapter<String>(
    searchCategoryDelegate(
        onCateoryClicked
    ),
    areItemsTheSame = { oldItem, newItem -> oldItem == newItem }
)
