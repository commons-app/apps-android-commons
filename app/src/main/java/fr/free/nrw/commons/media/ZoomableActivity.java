package fr.free.nrw.commons.media;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.media.zoomControllers.zoomable.DoubleTapGestureListener;
import fr.free.nrw.commons.media.zoomControllers.zoomable.ZoomableDraweeView;
import timber.log.Timber;

public class ZoomableActivity extends AppCompatActivity {

  private Uri imageUri;

  @BindView(R.id.zoomable)
  ZoomableDraweeView photo;
  @BindView(R.id.zoom_progress_bar)
  ProgressBar spinner;

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

  /**
   * Two types of loading indicators have been added to the zoom activity: 1.  An Indeterminate
   * spinner for showing the time lapsed between dispatch of the image request and starting to
   * receiving the image. 2.  ProgressBarDrawable that reflects how much image has been downloaded
   */
  private final ControllerListener loadingListener = new BaseControllerListener<ImageInfo>() {
    @Override
    public void onSubmit(String id, Object callerContext) {
      // Sometimes the spinner doesn't appear when rapidly switching between images, this fixes that
      spinner.setVisibility(View.VISIBLE);
    }

    @Override
    public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
      spinner.setVisibility(View.GONE);
    }

    @Override
    public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo,
        @Nullable Animatable animatable) {
      spinner.setVisibility(View.GONE);
    }
  };

  private void init() {
    if (imageUri != null) {
      GenericDraweeHierarchy hierarchy = GenericDraweeHierarchyBuilder.newInstance(getResources())
          .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
          .setProgressBarImage(new ProgressBarDrawable())
          .setProgressBarImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
          .build();
      photo.setHierarchy(hierarchy);
      photo.setAllowTouchInterceptionWhileZoomed(true);
      photo.setIsLongpressEnabled(false);
      photo.setTapListener(new DoubleTapGestureListener(photo));
      DraweeController controller = Fresco.newDraweeControllerBuilder()
          .setUri(imageUri)
          .setControllerListener(loadingListener)
          .build();
      photo.setController(controller);
    }
  }


}
