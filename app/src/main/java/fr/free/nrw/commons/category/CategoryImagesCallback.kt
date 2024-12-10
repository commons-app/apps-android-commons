package fr.free.nrw.commons.category

interface CategoryImagesCallback {
    fun viewPagerNotifyDataSetChanged()

    fun onMediaClicked(position: Int)
}