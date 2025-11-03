package fr.free.nrw.commons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import androidx.core.graphics.createBitmap

class BaseMarker {
    var position: LatLng = LatLng(0.0, 0.0, 0f)
    var title: String = ""
    var icon: Bitmap? = null
    var place: Place = Place()

    fun fromResource(context: Context, drawableResId: Int) {
        val drawable: Drawable? = AppCompatResources.getDrawable(context, drawableResId)
        icon = when (drawable) {
            null -> null
            is BitmapDrawable -> drawable.bitmap
            else -> {
                val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        }
    }
}
