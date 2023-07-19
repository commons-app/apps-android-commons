package fr.free.nrw.commons.description

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ActivityDescriptionEditBinding
import fr.free.nrw.commons.description.EditDescriptionConstants.LIST_OF_DESCRIPTION_AND_CAPTION
import fr.free.nrw.commons.description.EditDescriptionConstants.UPDATED_WIKITEXT
import fr.free.nrw.commons.description.EditDescriptionConstants.WIKITEXT
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import javax.inject.Inject

/**
 * Activity for populating and editing existing description and caption
 */
class DescriptionEditActivity : BaseActivity(), UploadMediaDetailAdapter.EventListener {
    /**
     * Adapter for showing UploadMediaDetail in the activity
     */
    private lateinit var uploadMediaDetailAdapter: UploadMediaDetailAdapter

    /**
     * Recyclerview for recycling data in views
     */
    @JvmField
    var rvDescriptions: RecyclerView? = null

    /**
     * Current wikitext
     */
    var wikiText: String? = null

    /**
     * Saved language
     */
    private lateinit var savedLanguageValue: String

    /**
     * For showing progress dialog
     */
    private var progressDialog: ProgressDialog? = null

    @Inject
    lateinit var recentLanguagesDao: RecentLanguagesDao

    private lateinit var binding: ActivityDescriptionEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDescriptionEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras
        val descriptionAndCaptions: ArrayList<UploadMediaDetail> =
            bundle!!.getParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION)!!
        wikiText = bundle.getString(WIKITEXT)
        savedLanguageValue = bundle.getString(Prefs.DESCRIPTION_LANGUAGE)!!
        initRecyclerView(descriptionAndCaptions)

        binding.btnAddDescription.setOnClickListener(::onButtonAddDescriptionClicked)
        binding.btnEditSubmit.setOnClickListener(::onSubmitButtonClicked)
        binding.toolbarBackButton.setOnClickListener(::onBackButtonClicked)
    }

    /**
     * Initializes the RecyclerView
     * @param descriptionAndCaptions list of description and caption
     */
    private fun initRecyclerView(descriptionAndCaptions: ArrayList<UploadMediaDetail>?) {
        uploadMediaDetailAdapter = UploadMediaDetailAdapter(
            savedLanguageValue, descriptionAndCaptions, recentLanguagesDao)
        uploadMediaDetailAdapter.setCallback { titleStringID: Int, messageStringId: Int ->
            showInfoAlert(
                titleStringID,
                messageStringId
            )
        }
        uploadMediaDetailAdapter.setEventListener(this)
        rvDescriptions = binding.rvDescriptionsCaptions
        rvDescriptions!!.layoutManager = LinearLayoutManager(this)
        rvDescriptions!!.adapter = uploadMediaDetailAdapter
    }

    /**
     * show dialog with info
     * @param titleStringID Title ID
     * @param messageStringId Message ID
     */
    private fun showInfoAlert(titleStringID: Int, messageStringId: Int) {
        showAlertDialog(
            this, getString(titleStringID),
            getString(messageStringId), getString(android.R.string.ok),
            null, true
        )
    }

    override fun onPrimaryCaptionTextChange(isNotEmpty: Boolean) {}

    private fun onBackButtonClicked(view: View) {
        onBackPressed()
    }

    private fun onButtonAddDescriptionClicked(view: View) {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.isManuallyAdded = true //This was manually added by the user
        uploadMediaDetailAdapter.addDescription(uploadMediaDetail)
        rvDescriptions!!.smoothScrollToPosition(uploadMediaDetailAdapter.itemCount - 1)
    }

    private fun onSubmitButtonClicked(view: View) {
        showLoggingProgressBar()
        val uploadMediaDetails = uploadMediaDetailAdapter.items
        updateDescription(uploadMediaDetails)
        finish()
    }

    /**
     * Updates newly added descriptions in the wikiText and send to calling fragment
     * @param uploadMediaDetails descriptions and captions
     */
    private fun updateDescription(uploadMediaDetails: List<UploadMediaDetail?>) {
        var descriptionIndex = wikiText!!.indexOf("description=")
        if (descriptionIndex == -1) {
            descriptionIndex = wikiText!!.indexOf("Description=")
        }
        val buffer = StringBuilder()
        if (descriptionIndex != -1) {
            val descriptionStart = wikiText!!.substring(0, descriptionIndex + 12)
            val descriptionToEnd = wikiText!!.substring(descriptionIndex + 12)
            val descriptionEndIndex = descriptionToEnd.indexOf("\n")
            val descriptionEnd = wikiText!!.substring(
                descriptionStart.length
                        + descriptionEndIndex
            )
            buffer.append(descriptionStart)
            for (i in uploadMediaDetails.indices) {
                val uploadDetails = uploadMediaDetails[i]
                if (uploadDetails!!.descriptionText != "") {
                    buffer.append("{{")
                    buffer.append(uploadDetails.languageCode)
                    buffer.append("|1=")
                    buffer.append(uploadDetails.descriptionText)
                    buffer.append("}}")
                }
            }
            buffer.replace(", $".toRegex(), "")
            buffer.append(descriptionEnd)
        }
        val returningIntent = Intent()
        returningIntent.putExtra(UPDATED_WIKITEXT, buffer.toString())
        returningIntent.putParcelableArrayListExtra(
            LIST_OF_DESCRIPTION_AND_CAPTION,
            uploadMediaDetails as ArrayList<out Parcelable?>
        )
        setResult(RESULT_OK, returningIntent)
        finish()
    }

    private fun showLoggingProgressBar() {
        progressDialog = ProgressDialog(this)
        progressDialog!!.isIndeterminate = true
        progressDialog!!.setTitle(getString(R.string.updating_caption_title))
        progressDialog!!.setMessage(getString(R.string.updating_caption_message))
        progressDialog!!.setCanceledOnTouchOutside(false)
        progressDialog!!.show()
    }
}