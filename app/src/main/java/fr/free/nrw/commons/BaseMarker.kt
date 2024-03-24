package fr.free.nrw.commons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place

class BaseMarker {
    private var _position: LatLng = LatLng(0.0, 0.0, 0f)
    private var _title: String = ""
    private var _place: Place = Place()
    private var _icon: Bitmap? = null

    var position: LatLng
        get() = _position
        set(value) {
            _position = value
        }
    var title: String
        get() = _title
        set(value) {
            _title = value
        }

    var place: Place
        get() = _place
        set(value) {
            _place = value
        }
    var icon: Bitmap?
        get() = _icon
        set(value) {
            _icon = value
        }

    constructor() {
    }

    fun fromResource(context: Context, drawableResId: Int) {
        val drawable: Drawable = context.resources.getDrawable(drawableResId)
        icon = if (drawable is BitmapDrawable) {
            (drawable as BitmapDrawable).bitmap
        } else {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }
}







