package fr.free.nrw.commons.nearby;

import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererViewHolder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDrawable;

import java.util.Collections;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class NearbyAdapterFactoryTest {

    private static final Place PLACE = new Place("name", Place.Description.AIRPORT,
            "desc", null, new LatLng(38.6270, -90.1994, 0), null);
    private static final Place UNKNOWN_PLACE = new Place("name", Place.Description.UNKNOWN,
            "desc", null, new LatLng(39.7392, -104.9903, 0), null);
    private Place clickedPlace;

    @Test
    public void factoryHandlesNullListAndNullListener() {
        NearbyAdapterFactory testObject = new NearbyAdapterFactory(null);
        RVRendererAdapter<Place> result = testObject.create(null);
        assertNotNull(result);
        assertEquals(0, result.getItemCount());
    }

    @Test
    public void factoryHandlesEmptyListAndNullListener() {
        NearbyAdapterFactory testObject = new NearbyAdapterFactory(null);
        RVRendererAdapter<Place> result = testObject.create(Collections.<Place>emptyList());
        assertNotNull(result);
        assertEquals(0, result.getItemCount());
    }

    @Test
    public void factoryHandlesNonEmptyListAndNullListener() {
        NearbyAdapterFactory testObject = new NearbyAdapterFactory(null);
        RVRendererAdapter<Place> result = testObject.create(Collections.singletonList(PLACE));
        assertNotNull(result);
        assertEquals(1, result.getItemCount());
        assertEquals(PLACE, result.getItem(0));
    }

    @Test
    public void rendererCorrectlyBound() {
        NearbyAdapterFactory testObject = new NearbyAdapterFactory(null);
        RVRendererAdapter<Place> result = testObject.create(Collections.singletonList(PLACE));

        RendererViewHolder viewHolder = result.onCreateViewHolder(new FrameLayout(RuntimeEnvironment.application), result.getItemViewType(0));
        assertNotNull(viewHolder);
        result.bindViewHolder(viewHolder, 0);

        assertNotNull(viewHolder.itemView.findViewById(R.id.tvName));
        assertEquals("name", ((TextView) viewHolder.itemView.findViewById(R.id.tvName)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.tvDesc));
        assertEquals("airport", ((TextView) viewHolder.itemView.findViewById(R.id.tvDesc)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.distance));
        assertEquals("", ((TextView) viewHolder.itemView.findViewById(R.id.distance)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.icon));
        ShadowDrawable shadow  = Shadows.shadowOf(((ImageView) viewHolder.itemView.findViewById(R.id.icon)).getDrawable());
        assertEquals(R.drawable.round_icon_airport, shadow.getCreatedFromResId());
    }

    @Test
    public void rendererCorrectlyBoundForUnknownPlace() {
        NearbyAdapterFactory testObject = new NearbyAdapterFactory(null);
        RVRendererAdapter<Place> result = testObject.create(Collections.singletonList(UNKNOWN_PLACE));

        RendererViewHolder viewHolder = result.onCreateViewHolder(new FrameLayout(RuntimeEnvironment.application), result.getItemViewType(0));
        assertNotNull(viewHolder);
        result.bindViewHolder(viewHolder, 0);

        assertNotNull(viewHolder.itemView.findViewById(R.id.tvName));
        assertEquals("name", ((TextView) viewHolder.itemView.findViewById(R.id.tvName)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.tvDesc));
        assertEquals("no description found", ((TextView) viewHolder.itemView.findViewById(R.id.tvDesc)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.distance));
        assertEquals("", ((TextView) viewHolder.itemView.findViewById(R.id.distance)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.icon));
        ShadowDrawable shadow  = Shadows.shadowOf(((ImageView) viewHolder.itemView.findViewById(R.id.icon)).getDrawable());
        assertEquals(R.drawable.round_icon_unknown, shadow.getCreatedFromResId());
    }

    @Test
    public void clickView() {
        NearbyAdapterFactory testObject = new NearbyAdapterFactory(new PlaceRenderer.PlaceClickedListener() {
            @Override
            public void placeClicked(Place place) {
                clickedPlace = place;
            }
        });
        RVRendererAdapter<Place> result = testObject.create(Collections.singletonList(PLACE));
        RendererViewHolder viewHolder = result.onCreateViewHolder(new FrameLayout(RuntimeEnvironment.application), result.getItemViewType(0));
        assertNotNull(viewHolder);
        result.bindViewHolder(viewHolder, 0);

        viewHolder.itemView.performClick();

        assertEquals(PLACE, clickedPlace);
    }

    @Test
    public void clickViewHandlesMisconfiguredListener() {
        NearbyAdapterFactory testObject = new NearbyAdapterFactory(null);
        RVRendererAdapter<Place> result = testObject.create(Collections.singletonList(PLACE));
        RendererViewHolder viewHolder = result.onCreateViewHolder(new FrameLayout(RuntimeEnvironment.application), result.getItemViewType(0));
        assertNotNull(viewHolder);
        result.bindViewHolder(viewHolder, 0);

        viewHolder.itemView.performClick();
    }
}