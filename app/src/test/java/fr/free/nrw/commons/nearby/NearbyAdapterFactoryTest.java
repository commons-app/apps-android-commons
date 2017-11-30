package fr.free.nrw.commons.nearby;

import android.support.annotation.NonNull;
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
import fr.free.nrw.commons.TestCommonsApplication;
import fr.free.nrw.commons.location.LatLng;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
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

        RendererViewHolder viewHolder = renderComponent(result);

        assertNotNull(viewHolder.itemView.findViewById(R.id.tvName));
        assertEquals("name",
                ((TextView) viewHolder.itemView.findViewById(R.id.tvName)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.tvDesc));
        assertEquals("airport",
                ((TextView) viewHolder.itemView.findViewById(R.id.tvDesc)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.distance));
        assertEquals("",
                ((TextView) viewHolder.itemView.findViewById(R.id.distance)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.icon));
        ImageView imageView = (ImageView) viewHolder.itemView.findViewById(R.id.icon);
        ShadowDrawable shadow  = Shadows.shadowOf(imageView.getDrawable());
        assertEquals(R.drawable.round_icon_airport, shadow.getCreatedFromResId());
    }

    @Test
    public void rendererCorrectlyBoundForUnknownPlace() {
        NearbyAdapterFactory testObject = new NearbyAdapterFactory(null);
        RVRendererAdapter<Place> result = testObject.create(Collections.singletonList(UNKNOWN_PLACE));

        RendererViewHolder viewHolder = renderComponent(result);

        assertNotNull(viewHolder.itemView.findViewById(R.id.tvDesc));
        assertEquals("no description found",
                ((TextView) viewHolder.itemView.findViewById(R.id.tvDesc)).getText().toString());

        assertNotNull(viewHolder.itemView.findViewById(R.id.icon));
        ImageView imageView = (ImageView) viewHolder.itemView.findViewById(R.id.icon);
        ShadowDrawable shadow  = Shadows.shadowOf(imageView.getDrawable());
        assertEquals(R.drawable.round_icon_unknown, shadow.getCreatedFromResId());
    }

    @Test
    public void clickView() {
        NearbyAdapterFactory testObject = new NearbyAdapterFactory(new MockPlaceClickedListener());
        RVRendererAdapter<Place> result = testObject.create(Collections.singletonList(PLACE));
        RendererViewHolder viewHolder = renderComponent(result);

        viewHolder.itemView.performClick();

        assertEquals(PLACE, clickedPlace);
    }

    @NonNull
    private RendererViewHolder renderComponent(RVRendererAdapter<Place> result) {
        FrameLayout viewGroup = new FrameLayout(RuntimeEnvironment.application);
        int itemViewType = result.getItemViewType(0);
        RendererViewHolder viewHolder = result.onCreateViewHolder(viewGroup, itemViewType);
        assertNotNull(viewHolder);
        result.bindViewHolder(viewHolder, 0);
        return viewHolder;
    }

    private class MockPlaceClickedListener implements PlaceRenderer.PlaceClickedListener {
        @Override
        public void placeClicked(Place place) {
            clickedPlace = place;
        }
    }
}