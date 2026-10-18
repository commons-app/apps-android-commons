package fr.free.nrw.commons.upload.license
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.LayoutUploadLicenseItemBinding
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.utils.toLicenseName
import fr.free.nrw.commons.utils.toLicenseUrl

class LicenseAdapter(
    private val licenseList: List<String>,
    private var selectedLicense: String?,
    private val onLicenseSelected: (String) -> Unit,
    private val onLinkClick: (String) -> Unit
) : RecyclerView.Adapter<LicenseAdapter.LicenseViewHolder>() {

    private var expandedPosition = -1

    fun setSelectedLicense(license: String?) {
        this.selectedLicense = license
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LicenseViewHolder {
        val itemBinding = LayoutUploadLicenseItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LicenseViewHolder(itemBinding)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: LicenseViewHolder, position: Int) {
        val licenseKey = licenseList[position]
        val isSelected = licenseKey == selectedLicense
        val isExpanded = position == expandedPosition
        val context = holder.itemView.context

        holder.binding.apply {
            licenseTitle.text = context.getString(licenseKey.toLicenseName())
            licenseShortDescription.text = getShortDesc(context, licenseKey)
            licenseLongDescription.text = getLongDesc(context, licenseKey)

            licenseRadioButton.isChecked = isSelected
            licenseCard.strokeColor = if (isSelected)
                context.getColor(R.color.button_blue)
            else context.getColor(R.color.divider_grey)

            longDescriptionContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expansionIcon.rotation = if (isExpanded) 180f else 0f

            root.setOnClickListener {
                selectedLicense = licenseKey
                onLicenseSelected(licenseKey)
                expandedPosition = if (isExpanded) -1 else holder.bindingAdapterPosition
                notifyDataSetChanged()
            }

            tvSeeMore.setOnClickListener {
                onLinkClick(licenseKey.toLicenseUrl())
            }
        }
    }

    override fun getItemCount() = licenseList.size

    private fun getShortDesc(context: android.content.Context, key: String) = when (key) {
        Prefs.Licenses.CC0, "CC0" -> context.getString(R.string.license_cc0_description)
        Prefs.Licenses.CC_BY_4, "Attribution 4.0" -> context.getString(R.string.license_cc_by_description)
        Prefs.Licenses.CC_BY_SA_4, "Attribution-ShareAlike 4.0" -> context.getString(R.string.license_cc_by_sa_description)
        // fallback for the3.0 or the other versions
        else -> "Standard Creative Commons license terms apply."
    }

    private fun getLongDesc(context: android.content.Context, key: String) = when (key) {
        Prefs.Licenses.CC0, "CC0" -> context.getString(R.string.license_cc0_long_description)
        Prefs.Licenses.CC_BY_4, "Attribution 4.0" -> context.getString(R.string.license_cc_by_long_description)
        Prefs.Licenses.CC_BY_SA_4, "Attribution-ShareAlike 4.0" -> context.getString(R.string.license_cc_by_sa_long_description)
        // Only show the older verison warnning to actual older versions
        else -> "This is an older version of the license. It is recommended to use the 4.0 versions for better international legal compatibility."
    }

    class LicenseViewHolder(val binding: LayoutUploadLicenseItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}