package fr.free.nrw.commons.achievements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class BitmapUtils {

    /**
     * Private constructor to hide the implicit one.
     * Since util classes only contains static methods, we shouldn't allow object creation.
     */
    private BitmapUtils(){}

    /**
     *  write level Number on the badge
     * @param bm
     * @param text
     * @return
     */
    public static BitmapDrawable writeOnDrawable(Bitmap bm, String text, Context context){
        Bitmap.Config config = bm.getConfig();
        if (config == null){
            config = Bitmap.Config.ARGB_8888;
        }
        Bitmap bitmap = Bitmap.createBitmap(bm.getWidth(),bm.getHeight(),config);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bm, 0, 0, null);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(canvas.getHeight()/2);
        paint.setTextAlign(Paint.Align.CENTER);
        Rect rectText = new Rect();
        paint.getTextBounds(text,0, text.length(),rectText);
        canvas.drawText(text, canvas.getWidth()/2,Math.round(canvas.getHeight()/1.35), paint);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /**
     * Convert Drawable to bitmap
     * @param drawable
     * @return
     */
    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
