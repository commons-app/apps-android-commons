package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
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

/**
 * Image Loader class, loads images, depending on API results.
 */
class ImageLoader constructor(var mediaClient: MediaClient,var fileProcessor: FileProcessor, val context: Context) {



    private var mapped: HashMap<Image,String> = HashMap()
    private var mapping2 : HashMap<ImageViewHolder,Image> = HashMap()
    private var mapResult: HashMap<String,Boolean> = HashMap()

    var load=false

    fun loadImageIntoImageView(holder : ImageViewHolder, image: Image){
        mapping2.put(holder,image)
        Glide.with(holder.itemView.context)
            .load(R.drawable.image_placeholder)
            .into(holder.image)
        holder.itemNotUploaded()
        CoroutineScope(Dispatchers.Main).launch {
            var value = false
            withContext(Dispatchers.IO) {
                if(mapping2.get(holder)!=image)
                    return@withContext
                var sha1=getSha1(image)
                mapped.put(image,sha1)
                if(mapping2.get(holder)!=image)
                    return@withContext
                value = querySha1(sha1)
                mapResult.put(sha1,value)
            }
            if(mapping2.get(holder)==image) {
                if (value == true) {
                    holder.itemUploaded()
                } else {
                        Glide.with(holder.itemView.context)
                            .load(image.uri)
                            .into(holder.image)
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
        if(mapped.get(image)!=null){
            return mapped.get(image)!!
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
        var sha1=ImageHelper.generateSHA1(ImageHelper.getFileInputStream(uploadableFile.filePath))
        uploadableFile.file.delete()
        return sha1
    }

}