package fr.free.nrw.commons.nearby.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentAdvanceQueryBinding
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.NearbySearchOptions

/**
 * Form for Nearby search choices. Builds [NearbySearchOptions] from the controls
 * and leaves the Wikidata query templates unchanged.
 */
class AdvanceQueryFragment : Fragment() {
    private var _binding: FragmentAdvanceQueryBinding? = null
    val binding get() = _binding

    lateinit var callback: Callback

    private var seekRadius: SeekBar? = null
    private var radiusValue: TextView? = null
    private var radiusTicks: LinearLayout? = null
    private var placeTypesHeader: View? = null
    private var placeTypesArrow: ImageView? = null
    private var placeTypesScroll: ScrollView? = null
    private var placeTypesContainer: LinearLayout? = null
    private var checkboxMissingImage: CheckBox? = null
    private var checkboxHideDestroyed: CheckBox? = null
    private var btnApply: Button? = null
    private var btnReset: Button? = null

    private val selectedLabels = LinkedHashSet<Label>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentAdvanceQueryBinding.inflate(inflater, container, false)
        seekRadius = binding?.seekRadius
        radiusValue = binding?.tvRadiusValue
        radiusTicks = binding?.radiusTicks
        placeTypesHeader = binding?.placeTypesHeader
        placeTypesArrow = binding?.placeTypesArrow
        placeTypesScroll = binding?.placeTypesScroll
        placeTypesContainer = binding?.placeTypesContainer
        checkboxMissingImage = binding?.checkboxMissingImage
        checkboxHideDestroyed = binding?.checkboxHideDestroyed
        btnApply = binding?.btnApply
        btnReset = binding?.btnReset
        setHasOptionsMenu(false)
        return binding?.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setUi()
        setClickListeners()
    }

    /**
     * Fills the radius slider and place-type rows from [options].
     */
    private fun setUi(options: NearbySearchOptions = readOptions()) {
        seekRadius?.max = NearbySearchOptions.RADIUS_SLIDER_MAX
        val progress = NearbySearchOptions.sliderProgressForRadius(options.radiusKm)
        seekRadius?.progress = progress
        showRadius(NearbySearchOptions.radiusKmForSlider(progress))

        selectedLabels.clear()
        placeTypesContainer?.removeAllViews()
        val savedNames = options.labelNames.toSet()
        Label.values().forEach { label ->
            if (label == Label.BOOKMARKS || label == Label.UNKNOWN) {
                return@forEach
            }
            addPlaceTypeRow(label, savedNames.contains(label.name))
        }
        checkboxMissingImage?.isChecked = options.missingImageOnly
        checkboxHideDestroyed?.isChecked = options.excludeDestroyed
        val ticks = radiusTicks ?: return
        ticks.removeAllViews()
        val labels =
            listOf(getString(R.string.nearby_search_slider_map)) +
                NearbySearchOptions.RADIUS_CHOICES_KM.map { kilometers ->
                    getString(R.string.nearby_search_radius_km, kilometers.toInt())
                }
        val textColor = ContextCompat.getColor(requireContext(), R.color.secondaryTextColor)
        labels.forEach { label ->
            val tick =
                AppCompatTextView(requireContext()).apply {
                    text = label
                    gravity = Gravity.CENTER
                    setTextColor(textColor)
                    textSize = TICK_TEXT_SIZE_SP
                    layoutParams =
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
            ticks.addView(tick)
        }
    }

    /**
     * Shows either the current map view or the radius at [radiusKm].
     */
    private fun showRadius(radiusKm: Double?) {
        radiusValue?.text =
            if (radiusKm == null) {
                getString(R.string.nearby_search_map_area)
            } else if (radiusKm % 1.0 == 0.0) {
                getString(R.string.nearby_search_radius_km, radiusKm.toInt())
            } else {
                getString(R.string.nearby_search_radius_km_precise, radiusKm)
            }
    }

    /**
     * Adds one place-type row. Selection stays on this screen and does not change
     * [Label.isSelected], which the map filter list uses.
     */
    private fun addPlaceTypeRow(
        label: Label,
        selected: Boolean,
    ) {
        val row =
            layoutInflater.inflate(
                R.layout.nearby_search_list_item,
                placeTypesContainer,
                false,
            )
        row.findViewById<ImageView>(R.id.place_icon).setImageResource(label.icon)
        row.findViewById<TextView>(R.id.place_text).apply {
            text = label.toString()
            setTextColor(ContextCompat.getColor(requireContext(), R.color.secondaryTextColor))
        }
        val selectedIcon = row.findViewById<ImageView>(R.id.place_selected)
        if (selected) {
            selectedLabels.add(label)
            selectedIcon.visibility = View.VISIBLE
        }
        row.setOnClickListener {
            if (!selectedLabels.add(label)) {
                selectedLabels.remove(label)
            }
            selectedIcon.visibility = if (label in selectedLabels) View.VISIBLE else View.GONE
        }
        placeTypesContainer?.addView(row)
    }

    private fun setClickListeners() {
        seekRadius?.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    showRadius(NearbySearchOptions.radiusKmForSlider(progress))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            },
        )
        placeTypesHeader?.setOnClickListener {
            val expanded = placeTypesScroll?.visibility != View.VISIBLE
            placeTypesScroll?.visibility = if (expanded) View.VISIBLE else View.GONE
            placeTypesArrow?.rotation = if (expanded) EXPANDED_ARROW_DEGREES else 0f
        }
        placeTypesScroll?.setOnTouchListener { scrolled, _ ->
            scrolled.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        btnReset?.setOnClickListener {
            setUi(NearbySearchOptions())
            callback.reset()
        }
        btnApply?.setOnClickListener {
            callback.apply(currentOptions())
        }
    }

    /**
     * Reads the choices currently shown on the form.
     */
    private fun currentOptions(): NearbySearchOptions {
        val radiusKm = NearbySearchOptions.radiusKmForSlider(seekRadius?.progress ?: 0)
        val labels = selectedLabels.toList()
        return NearbySearchOptions(
            radiusKm,
            labels.flatMap { it.getQids(resources) }.distinct(),
            labels.map { it.name },
            false,
            checkboxMissingImage?.isChecked == true,
            checkboxHideDestroyed?.isChecked == true,
        )
    }

    @Suppress("DEPRECATION")
    private fun readOptions(): NearbySearchOptions =
        arguments?.getSerializable(NearbySearchOptions.ARG_KEY) as? NearbySearchOptions
            ?: NearbySearchOptions()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TICK_TEXT_SIZE_SP = 11f
        private const val EXPANDED_ARROW_DEGREES = 180f
    }

    interface Callback {
        /**
         * Clears the saved search choices and reloads Nearby. The form stays open.
         */
        fun reset()

        /**
         * Applies [options] to the next Nearby searches.
         */
        fun apply(options: NearbySearchOptions)
    }
}
