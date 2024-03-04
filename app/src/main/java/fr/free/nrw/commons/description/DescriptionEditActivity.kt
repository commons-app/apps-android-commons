package fr.free.nrw.commons.description

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.speech.RecognizerIntent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ActivityDescriptionEditBinding
import fr.free.nrw.commons.description.EditDescriptionConstants.LIST_OF_DESCRIPTION_AND_CAPTION
import fr.free.nrw.commons.description.EditDescriptionConstants.WIKITEXT
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
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
     * Media object
     */
    var media: Media? = null

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

    private val REQUEST_CODE_FOR_VOICE_INPUT = 1213

    private var descriptionAndCaptions: ArrayList<UploadMediaDetail>? = null

    @Inject lateinit var descriptionEditHelper: DescriptionEditHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDescriptionEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras

        if (savedInstanceState != null) {
            descriptionAndCaptions = savedInstanceState.getParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION)
            wikiText = savedInstanceState.getString(WIKITEXT)
            savedLanguageValue = savedInstanceState.getString(Prefs.DESCRIPTION_LANGUAGE)!!
            media = savedInstanceState.getParcelable("media")
        } else {
            descriptionAndCaptions =
                bundle!!.getParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION)!!
            wikiText = bundle.getString(WIKITEXT)
            savedLanguageValue = bundle.getString(Prefs.DESCRIPTION_LANGUAGE)!!
            media = bundle.getParcelable("media")
        }
        initRecyclerView(descriptionAndCaptions)

        binding.btnEditSubmit.setOnClickListener(::onSubmitButtonClicked)
        binding.toolbarBackButton.setOnClickListener(::onBackButtonClicked)
    }

    /**
     * Initializes the RecyclerView
     * @param descriptionAndCaptions list of description and caption
     */
    private fun initRecyclerView(descriptionAndCaptions: ArrayList<UploadMediaDetail>?) {
        uploadMediaDetailAdapter = UploadMediaDetailAdapter(this,
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

    /**
     * Adds new language item to RecyclerView
     */
    override fun addLanguage() {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.isManuallyAdded = true //This was manually added by the user
        uploadMediaDetailAdapter.addDescription(uploadMediaDetail)
        rvDescriptions!!.smoothScrollToPosition(uploadMediaDetailAdapter.itemCount - 1)
    }

    private fun onBackButtonClicked(view: View) {
       onBackPressedDispatcher.onBackPressed()
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
        editDescription(media!!, buffer.toString(), uploadMediaDetails as ArrayList<UploadMediaDetail>)

        finish()
    }

    /**
     * Edits description and caption
     * @param media media object
     * @param updatedWikiText updated wiki text
     * @param uploadMediaDetails descriptions and captions
     */
    private fun editDescription(media : Media, updatedWikiText : String, uploadMediaDetails : ArrayList<UploadMediaDetail>){
        descriptionEditHelper?.addDescription(
            applicationContext, media,
            updatedWikiText
        )
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(Consumer<Boolean> { s: Boolean? -> Timber.d("Descriptions are added.") })?.let {
                compositeDisposable.add(
                    it
            )
            }

        val updatedCaptions = LinkedHashMap<String, String>()
        for (mediaDetail in uploadMediaDetails) {
            compositeDisposable.add(
                descriptionEditHelper!!.addCaption(
                    applicationContext, media,
                    mediaDetail.languageCode, mediaDetail.captionText
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { s: Boolean? ->
                        updatedCaptions[mediaDetail.languageCode!!] = mediaDetail.captionText
                        media.captions = updatedCaptions
                        Timber.d("Caption is added.")
                    })
        }
    }

    private fun showLoggingProgressBar() {
        progressDialog = ProgressDialog(this)
        progressDialog!!.isIndeterminate = true
        progressDialog!!.setTitle(getString(R.string.updating_caption_title))
        progressDialog!!.setMessage(getString(R.string.updating_caption_message))
        progressDialog!!.setCanceledOnTouchOutside(false)
        progressDialog!!.show()
    }

    override
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FOR_VOICE_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra( RecognizerIntent.EXTRA_RESULTS )
                uploadMediaDetailAdapter.handleSpeechResult(result!![0]) }
            else { Timber.e("Error %s", resultCode) }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION, uploadMediaDetailAdapter.items as ArrayList<out Parcelable?>)
        outState.putString(WIKITEXT, wikiText)
        outState.putString(Prefs.DESCRIPTION_LANGUAGE, savedLanguageValue)
        //save Media
        outState.putParcelable("media", media)
    }
}
