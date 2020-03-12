package fr.free.nrw.commons.upload

import fr.free.nrw.commons.utils.ImageUtils
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * We want to discourage users from uploading images to Commons that were taken from Facebook. This
 * attempts to detect whether an image was downloaded from Facebook by heuristically searching for
 * metadata that is specific to images that come from Facebook.
 */
@Singleton
class ReadFBMD @Inject constructor() {
    fun processMetadata(path: String?) = Single.fromCallable {
        try {
            val fileStr = FileInputStream(path).use {
                ByteArray(4096).apply { it.read(this) }.toString()
            }
            if (isFbmd(fileStr.indexOf("8BIM"), fileStr.indexOf("FBMD")))
                return@fromCallable ImageUtils.FILE_FBMD
        } catch (e: IOException) {
            e.printStackTrace()
        }
        ImageUtils.IMAGE_OK
    }.subscribeOn(Schedulers.io())

    private fun isFbmd(psBlockOffset: Int, fbmdOffset: Int) =
        psBlockOffset > 0 && fbmdOffset > 0 &&
                fbmdOffset > psBlockOffset &&
                fbmdOffset - psBlockOffset < 0x80
}
