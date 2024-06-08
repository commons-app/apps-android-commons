package fr.free.nrw.commons.upload


data class PendingUploadItem(var title: String, var image: String, var queued : Boolean ,var error:String)