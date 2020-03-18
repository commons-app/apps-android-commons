package fr.free.nrw.commons.media;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.media.zoomControllers.zoomable.DoubleTapGestureListener;
import fr.free.nrw.commons.media.zoomControllers.zoomable.ZoomableDraweeView;
import timber.log.Timber;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ZoomableActivity extends AppCompatActivity {
    private Uri imageUri;

    @BindView(R.id.zoomable)
    ZoomableDraweeView photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imageUri = getIntent().getData();
        if (null == imageUri) {
            throw new IllegalArgumentException("No data to display");
        }
        Timber.d("URl = " + imageUri);

        setContentView(R.layout.activity_zoomable);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        if( imageUri != null ) {
            photo.setAllowTouchInterceptionWhileZoomed(true);
            photo.setIsLongpressEnabled(false);
            photo.setTapListener(new DoubleTapGestureListener(photo));
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setUri(imageUri)
                    .build();
            photo.setController(controller);
        }
    }


}
