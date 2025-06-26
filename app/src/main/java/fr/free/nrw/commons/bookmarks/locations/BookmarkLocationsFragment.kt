package fr.free.nrw.commons.bookmarks.locations

import android.Manifest.permission
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.databinding.FragmentBookmarksLocationsBinding
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.CommonPlaceClickActions
import fr.free.nrw.commons.nearby.fragments.PlaceAdapter
import kotlinx.coroutines.launch
import javax.inject.Inject


class BookmarkLocationsFragment : DaggerFragment() {

    private var binding: FragmentBookmarksLocationsBinding? = null

    @Inject lateinit var controller: BookmarkLocationsController
    @Inject lateinit var contributionController: ContributionController
    @Inject lateinit var bookmarkLocationDao: BookmarkLocationsDao
    @Inject lateinit var commonPlaceClickActions: CommonPlaceClickActions

    private lateinit var inAppCameraLocationPermissionLauncher:
            ActivityResultLauncher<Array<String>>
    private lateinit var adapter: PlaceAdapter

    private val cameraPickLauncherForResult =
        registerForActivityResult(StartActivityForResult()) { result ->
            contributionController.handleActivityResultWithCallback(
                requireActivity()
            ) { callbacks ->
                contributionController.onPictureReturnedFromCamera(
                    result,
                    requireActivity(),
                    callbacks
                )
            }
        }

    private val galleryPickLauncherForResult =
        registerForActivityResult(StartActivityForResult()) { result ->
            contributionController.handleActivityResultWithCallback(
                requireActivity()
            ) { callbacks ->
                contributionController.onPictureReturnedFromGallery(
                    result,
                    requireActivity(),
                    callbacks
                )
            }
        }

    companion object {
        fun newInstance(): BookmarkLocationsFragment {
            return BookmarkLocationsFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBookmarksLocationsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.loadingImagesProgressBar?.visibility = View.VISIBLE
        binding?.listView?.layoutManager = LinearLayoutManager(context)

        inAppCameraLocationPermissionLauncher =
            registerForActivityResult(RequestMultiplePermissions()) { result ->
                val areAllGranted = result.values.all { it }

                if (areAllGranted) {
                    contributionController.locationPermissionCallback?.onLocationPermissionGranted()
                } else {
                    if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                        contributionController.handleShowRationaleFlowCameraLocation(
                            requireActivity(),
                            inAppCameraLocationPermissionLauncher,
                            cameraPickLauncherForResult
                        )
                    } else {
                        contributionController.locationPermissionCallback
                            ?.onLocationPermissionDenied(
                                getString(R.string.in_app_camera_location_permission_denied)
                            )
                    }
                }
            }

        adapter = PlaceAdapter(
            bookmarkLocationDao,
            lifecycleScope,
            { },
            { place, _ ->
                adapter.remove(place)
            },
            commonPlaceClickActions,
            inAppCameraLocationPermissionLauncher,
            galleryPickLauncherForResult,
            cameraPickLauncherForResult
        )
        binding?.listView?.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        initList()
    }

    fun initList() {
        var places: List<Place>
        if(view != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                places = controller.loadFavoritesLocations()
                updateUIList(places)
            }
        }
    }

    private fun updateUIList(places: List<Place>) {
        adapter.items = places
        binding?.loadingImagesProgressBar?.visibility = View.GONE
        if (places.isEmpty()) {
            binding?.statusMessage?.text = getString(R.string.bookmark_empty)
            binding?.statusMessage?.visibility = View.VISIBLE
        } else {
            binding?.statusMessage?.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to null out the binding to avoid memory leaks
        binding = null
    }
}
