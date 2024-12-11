package fr.free.nrw.commons.feedback

import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.WindowManager
import android.widget.Toast
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.DialogFeedbackBinding
import fr.free.nrw.commons.feedback.model.Feedback
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import fr.free.nrw.commons.utils.DeviceInfoUtil.getAPILevel
import fr.free.nrw.commons.utils.DeviceInfoUtil.getAndroidVersion
import fr.free.nrw.commons.utils.DeviceInfoUtil.getConnectionType
import fr.free.nrw.commons.utils.DeviceInfoUtil.getDevice
import fr.free.nrw.commons.utils.DeviceInfoUtil.getDeviceManufacturer
import fr.free.nrw.commons.utils.DeviceInfoUtil.getDeviceModel

class FeedbackDialog(
    context: Context,
    private val onFeedbackSubmitCallback: OnFeedbackSubmitCallback) : Dialog(context) {
    private var _binding: DialogFeedbackBinding? = null
    private val binding get() = _binding!!
    // TODO("Remove Deprecation") Issue : #6002
    // 'fromHtml(String!): Spanned!' is deprecated. Deprecated in Java
    @Suppress("DEPRECATION")
    private var feedbackDestinationHtml: Spanned = Html.fromHtml(
        context.getString(R.string.feedback_destination_note))


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DialogFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.feedbackDestination.text = feedbackDestinationHtml
        binding.feedbackDestination.movementMethod = LinkMovementMethod.getInstance()
        // TODO("DEPRECATION") Issue : #6002
        // 'SOFT_INPUT_ADJUST_RESIZE: Int' is deprecated. Deprecated in Java
        @Suppress("DEPRECATION")
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding.btnSubmitFeedback.setOnClickListener {
            if (isInternetConnectionAvailable(context) ) {
                submitFeedback()
            }
            else {
                Toast.makeText(context,R.string.error_feedback, Toast.LENGTH_SHORT).show()
            }
        }
    }


    /**
     * This method is to check whether internet connection is available or not
     */
    fun isInternetConnectionAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork: Network? = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            return if (networkCapabilities != null) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidation = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val downlinkBandwidth = networkCapabilities.linkDownstreamBandwidthKbps
                val uplinkBandwidth = networkCapabilities.linkUpstreamBandwidthKbps
                val isBandwidthSufficient = downlinkBandwidth >= 150 && uplinkBandwidth >= 100

                hasInternet && hasValidation && isBandwidthSufficient
            } else {
                false
            }
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }



    fun submitFeedback() {
        if (binding.feedbackItemEditText.getText().toString() == "") {
            binding.feedbackItemEditText.error = context.getString(R.string.enter_description)
            return
        }
        val appVersion = context.getVersionNameWithSha()
        val androidVersion =
            if (binding.androidVersionCheckbox.isChecked) getAndroidVersion() else null
        val apiLevel =
            if (binding.apiLevelCheckbox.isChecked) getAPILevel() else null
        val deviceManufacturer =
            if (binding.deviceManufacturerCheckbox.isChecked) getDeviceManufacturer() else null
        val deviceModel =
            if (binding.deviceModelCheckbox.isChecked) getDeviceModel() else null
        val deviceName =
            if (binding.deviceNameCheckbox.isChecked) getDevice() else null
        val networkType =
            if (binding.networkTypeCheckbox.isChecked) getConnectionType(
                context
            ).toString() else null
        val feedback = Feedback(
            appVersion, apiLevel,
            binding.feedbackItemEditText.getText().toString(),
            androidVersion, deviceModel, deviceManufacturer, deviceName, networkType
        )
        onFeedbackSubmitCallback.onFeedbackSubmit(feedback)
        dismiss()
    }

    override fun dismiss() {
        super.dismiss()
        _binding = null
    }
}