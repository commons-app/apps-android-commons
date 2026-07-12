package fr.free.nrw.commons.upload.depicts

import android.os.Bundle
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ActivityDepictsPickerBinding
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets

class DepictsPickerActivity : BaseActivity() {

    @JvmField
    var binding: ActivityDepictsPickerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepictsPickerBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        applyEdgeToEdgeAllInsets(binding!!.root)
        if (savedInstanceState == null) {
            val photoLatLngs =
                intent.getParcelableArrayListExtra<LatLng>(
                    NearbyParentFragment.ARG_PHOTO_LATLNGS
                )
            val pickerFragment = NearbyParentFragment.newPickerInstance(photoLatLngs)

            supportFragmentManager.beginTransaction()
                .replace(R.id.picker_fragment_container, pickerFragment)
                .commit()
        }
    }


    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}
