package fr.free.nrw.commons.nearby

import android.os.Parcel
import com.mapbox.mapboxsdk.annotations.Icon
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox

class NearbyBaseMarkerUnitTests {

    private lateinit var marker: NearbyBaseMarker

    @Mock
    private lateinit var place: Place

    @Mock
    private lateinit var parcel: Parcel

    @Mock
    private lateinit var icon: Icon

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        marker = NearbyBaseMarker()
        Whitebox.setInternalState(marker, "icon", icon)
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(marker)
    }

    @Test
    @Throws(Exception::class)
    fun testPlace() {
       marker.place(place)
    }

    @Test
    @Throws(Exception::class)
    fun testGetThis() {
        marker.getThis()
    }

    @Test
    @Throws(Exception::class)
    fun testGetMarker() {
        marker.marker
    }

    @Test
    @Throws(Exception::class)
    fun testGetPlace() {
        marker.place
    }

    @Test
    @Throws(Exception::class)
    fun testDescribeContents() {
        marker.describeContents()
    }

    @Test
    @Throws(Exception::class)
    fun testWriteToParcel() {
        marker.writeToParcel(parcel, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testEquals() {
        marker.equals(this)
    }

    @Test
    @Throws(Exception::class)
    fun testEqualsCaseNull() {
        Assert.assertFalse(marker.equals(this))
    }

    @Test
    @Throws(Exception::class)
    fun testHashCode() {
        marker.hashCode()
    }

}