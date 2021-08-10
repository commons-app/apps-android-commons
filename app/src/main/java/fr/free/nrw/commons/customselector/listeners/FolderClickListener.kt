package fr.free.nrw.commons.customselector.listeners

/**
 * Custom Selector Folder Click Listener
 */
interface FolderClickListener {

    /**
     * onFolderClick
     * @param folderId : folder id of the folder.
     * @param folderName : folder name of the folder.
     * @param lastItemId : last scroll position in the folder.
     */
    fun onFolderClick(folderId: Long, folderName: String, lastItemId: Long)
}