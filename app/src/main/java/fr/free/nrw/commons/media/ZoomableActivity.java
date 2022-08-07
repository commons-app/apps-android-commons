package fr.free.nrw.commons.media;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
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
import fr.free.nrw.commons.customselector.helper.ImageHelper;
import fr.free.nrw.commons.customselector.helper.OnSwipeTouchListener;
import fr.free.nrw.commons.customselector.model.Image;
import fr.free.nrw.commons.customselector.ui.selector.FullScreenModeClient;
import fr.free.nrw.commons.media.zoomControllers.zoomable.DoubleTapGestureListener;
import fr.free.nrw.commons.media.zoomControllers.zoomable.ZoomableDraweeView;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.CustomSelectorUtils;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import javax.inject.Inject;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import timber.log.Timber;


public class ZoomableActivity extends BaseActivity {
    private Uri imageUri;

    @BindView(R.id.zoomable)
    ZoomableDraweeView photo;
    @BindView(R.id.zoom_progress_bar)
    ProgressBar spinner;
    @BindView(R.id.selection_count)
    TextView selectedCount;

    private ArrayList<Image> images;
    private ArrayList<Image> selectedImages;
    private int position;

    @Inject
    FullScreenModeClient fullScreenModeClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        images = getIntent().getParcelableArrayListExtra(
            "a");
        selectedImages = getIntent().getParcelableArrayListExtra(
            "c");
        position = getIntent().getIntExtra("b", 0);

        imageUri = images.get(position).getUri();
        if (null == imageUri) {
            throw new IllegalArgumentException("No data to display");
        }
        Timber.d("URl = " + imageUri);

        setContentView(R.layout.activity_zoomable);
        ButterKnife.bind(this);
        init(imageUri);
        onSwap();
    }

    private void onSwap() {
        photo.setOnTouchListener(new OnSwipeTouchListener(this){
            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                Toast.makeText(ZoomableActivity.this, "lefffffffttttt", Toast.LENGTH_SHORT).show();
                if (position < images.size()-1) {
                    position++;
                    init(images.get(position).getUri());
//                    int selectedIndex = getIndex(selectedImages, images.get(position));
//                    boolean isSelected = (selectedIndex != -1);
//                    Log.d("haha", "selectedIndex: "+isSelected);
//                    if (isSelected) {
//                        itemSelected(selectedIndex + 1);
//                    } else {
//                        itemUnselected();
//                    }
                } else {
                    Toast.makeText(ZoomableActivity.this, "No more images found", Toast.LENGTH_SHORT).show();
                }
                Log.d("haha", "onSwipeLeft: "+position);
            }
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                Toast.makeText(ZoomableActivity.this, "righhhhhhttttt", Toast.LENGTH_SHORT).show();
                if(position > 0) {
                    position--;
                    init(images.get(position).getUri());
//                    int selectedIndex = getIndex(selectedImages, images.get(position));
//                    boolean isSelected = (selectedIndex != -1);
//                    if (isSelected) {
//                        itemSelected(selectedIndex + 1);
//                    } else {
//                        itemUnselected();
//                    }
                } else {
                    Toast.makeText(ZoomableActivity.this, "No more images found", Toast.LENGTH_SHORT).show();
                }
                Log.d("haha", "onSwipeRight: "+position);
            }

            @Override
            public void onSwipeUp() {
                super.onSwipeUp();
                Toast.makeText(ZoomableActivity.this, "Uuuuuupppppppp", Toast.LENGTH_SHORT).show();
                selectedImages.add(images.get(position));
//                fullScreenModeClient.aa(selectedImages);
            }

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
                Toast.makeText(ZoomableActivity.this, "Dowwwwwwnnnnn", Toast.LENGTH_SHORT).show();
//                BuildersKt.launch(
//                    GlobalScope.INSTANCE,
//                    (CoroutineContext) Dispatchers.getMain(),//context to be ran on
//                    CoroutineStart.DEFAULT,
//                    new Function2<CoroutineScope, Continuation<? super Unit>, Object>() {
//                        @Override
//                        public Object invoke(CoroutineScope coroutineScope,
//                            Continuation<? super Unit> continuation) {
//                            Uri imageSHA1 = CustomSelectorUtils.Companion.getImageSHA1(
//                                images.get(position).getUri(),
//                                ,
//                                fileUtilsWrapper,
//                                contentResolver)
//                            return null;
//                        }
//                    }
//                );
                fullScreenModeClient.insertInNFU(images.get(position));
                if (position < images.size()-1) {
                    position++;
                    init(images.get(position).getUri());
//                    int selectedIndex = getIndex(selectedImages, images.get(position));
//                    boolean isSelected = (selectedIndex != -1);
//                    Log.d("haha", "selectedIndex: "+isSelected);
//                    if (isSelected) {
//                        itemSelected(selectedIndex + 1);
//                    } else {
//                        itemUnselected();
//                    }
                } else {
                    Toast.makeText(ZoomableActivity.this, "No more images found", Toast.LENGTH_SHORT).show();
                }
                Log.d("haha", "onSwipeDown: ");
            }
        });
    }

    private void itemUnselected() {
        selectedCount.setVisibility(View.INVISIBLE);
    }

    private void itemSelected(int i) {
        selectedCount.setVisibility(View.VISIBLE);
        selectedCount.setText(Integer.toString(i));
    }

    private int getIndex(ArrayList<Image> list, Image image) {
        return list.indexOf(image);
    }

    /**
     * Two types of loading indicators have been added to the zoom activity:
     *  1.  An Indeterminate spinner for showing the time lapsed between dispatch of the image request
     *      and starting to receiving the image.
     *  2.  ProgressBarDrawable that reflects how much image has been downloaded
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
        public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
            spinner.setVisibility(View.GONE);
        }
    };
    private void init(Uri imageUri) {
        if( imageUri != null ) {
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
            int selectedIndex = getIndex(selectedImages, images.get(position));
            boolean isSelected = (selectedIndex != -1);
            if (isSelected) {
                itemSelected(selectedIndex + 1);
            } else {
                itemUnselected();
            }
        }
    }

}
