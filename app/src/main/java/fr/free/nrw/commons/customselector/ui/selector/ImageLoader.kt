package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.FolderAdapter.FolderViewHolder
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter.ImageViewHolder
import fr.free.nrw.commons.filepicker.PickedFiles
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

/**
 * Image Loader class, loads images, depending on API results.
 */
class ImageLoader constructor(

    /**
     * MediaClient for query sha1
     */
    var mediaClient: MediaClient,

    /**
     * FileProcessor to pre-process the file
     */
    var fileProcessor: FileProcessor,

    /**
     * Context for coroutine.
     */
    val context: Context) {


    private var mapImageSha1: HashMap<Image,String> = HashMap()
    private var mapHolderImage : HashMap<ImageViewHolder,Image> = HashMap()
    private var mapResult: HashMap<String,Boolean> = HashMap()

    fun loadImageIntoImageView(holder : ImageViewHolder, image: Image){
        mapHolderImage.put(holder,image)
        holder.itemNotUploaded()
        CoroutineScope(Dispatchers.Main).launch {
            var value = false
            withContext(Dispatchers.Default) {
                if(mapHolderImage.get(holder)!=image) {
                    return@withContext
                }
                val sha1=getSha1(image)
                mapImageSha1.put(image,sha1)
                if(mapHolderImage.get(holder)!=image) {
                    return@withContext
                }
                value = querySha1(sha1)
                mapResult.put(sha1,value)
            }
            if(mapHolderImage.get(holder)==image) {
                if (value == true) {
                    holder.itemUploaded()
                } else {
                    holder.itemNotUploaded()
                }
            }
        }
    }


    fun loadImageIntoFolderView(holder: FolderViewHolder, image: Image){
        Glide.with(holder.itemView.context)
            .load(image.uri)
            .into(holder.image)
    }

    fun querySha1(sha1:String): Boolean {
        if(mapResult.get(sha1)!=null)
            return mapResult.get(sha1)!!
        return mediaClient.checkFileExistsUsingSha(sha1).blockingGet()
    }

    fun getSha1(image: Image): String{
        if(mapImageSha1.get(image)!=null){
            return mapImageSha1.get(image)!!
        }
        else{
            return generateModifiedSha1(image)
        }
    }

    fun generateModifiedSha1(image: Image) : String {
        var uploadableFile = PickedFiles.pickedExistingPicture(context,image.uri)
        val exifInterface: ExifInterface? = try {
            ExifInterface(uploadableFile.file!!)
        } catch (e: IOException) {
            Timber.e(e)
            null
        }
        fileProcessor.redactExifTags(exifInterface,fileProcessor.getExifTagsToRedact())
        val sha1=ImageHelper.generateSHA1(ImageHelper.getFileInputStream(uploadableFile.filePath))
        uploadableFile.file.delete()
        return sha1
    }

}