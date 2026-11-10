package fr.free.nrw.commons.description

import android.os.Bundle
import android.os.Parcelable
import android.speech.RecognizerIntent
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.databinding.ActivityDescriptionEditBinding
import fr.free.nrw.commons.description.EditDescriptionConstants.LIST_OF_DESCRIPTION_AND_CAPTION
import fr.free.nrw.commons.description.EditDescriptionConstants.WIKITEXT
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.applyEdgeToEdgeBottomInsets
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import fr.free.nrw.commons.utils.applyEdgeToEdgeTopPaddingInsets
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity for populating and editing existing description and caption
 */
class DescriptionEditActivity :
    BaseActivity(),
    UploadMediaDetailAdapter.EventListener {
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
    private val dialogs = DescriptionEditDialogs(this)

    @Inject
    lateinit var recentLanguagesDao: RecentLanguagesDao

    private lateinit var binding: ActivityDescriptionEditBinding

    private var descriptionAndCaptions: MutableList<UploadMediaDetail>? = null

    private val voiceInputResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        onVoiceInput(result)
    }

    @Inject lateinit var descriptionEditHelper: DescriptionEditHelper

    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDescriptionEditBinding.inflate(layoutInflater)
        applyEdgeToEdgeBottomInsets(binding.btnEditSubmit)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
        binding.toolbar.applyEdgeToEdgeTopPaddingInsets()
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
        binding.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Initializes the RecyclerView
     * @param descriptionAndCaptions list of description and caption
     */
    private fun initRecyclerView(descriptionAndCaptions: MutableList<UploadMediaDetail>?) {
        uploadMediaDetailAdapter =
            UploadMediaDetailAdapter(
                this,
                savedLanguageValue,
                descriptionAndCaptions ?: mutableListOf(),
                recentLanguagesDao,
                voiceInputResultLauncher
            )

        uploadMediaDetailAdapter.callback = UploadMediaDetailAdapter.Callback(dialogs::showInfoAlert)
        uploadMediaDetailAdapter.eventListener = this
        rvDescriptions = binding.rvDescriptionsCaptions
        rvDescriptions!!.layoutManager = LinearLayoutManager(this)
        rvDescriptions!!.adapter = uploadMediaDetailAdapter
    }

    override fun onPrimaryCaptionTextChange(isNotEmpty: Boolean) = Unit

    override fun onMediaDetailsChanged() = Unit

    private fun onVoiceInput(result: ActivityResult) {
        if (result.resultCode == RESULT_OK && result.data != null) {
            val resultData = result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            uploadMediaDetailAdapter.handleSpeechResult(resultData!![0])
        } else {
            Timber.e("Error %s", result.resultCode)
        }
    }

    /**
     * Adds new language item to RecyclerView
     */
    override fun addLanguage() {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.isManuallyAdded = true // This was manually added by the user
        uploadMediaDetailAdapter.addDescription(uploadMediaDetail)
        rvDescriptions!!.smoothScrollToPosition(uploadMediaDetailAdapter.itemCount - 1)
    }

    // View.OnClickListener method reference, see onCreate
    @Suppress("UnusedParameter")
    private fun onSubmitButtonClicked(view: View) {
        dialogs.showLoggingProgressBar()
        val uploadMediaDetails = uploadMediaDetailAdapter.items
        updateDescription(uploadMediaDetails)
        finish()
    }

    /**
     * Updates newly added descriptions in the wikiText and send to calling fragment
     * @param uploadMediaDetails descriptions and captions
     */
    private fun updateDescription(uploadMediaDetails: List<UploadMediaDetail?>) {
        val details = uploadMediaDetails.filterNotNull()
        val updatedWikiText = replaceDescriptionParameter(wikiText!!, details)
        editDescription(media!!, updatedWikiText, ArrayList(details))
        finish()
    }

    /**
     * Edits description and caption
     * @param media media object
     * @param updatedWikiText updated wiki text
     * @param uploadMediaDetails descriptions and captions
     */
    private fun editDescription(
        media: Media,
        updatedWikiText: String,
        uploadMediaDetails: ArrayList<UploadMediaDetail>,
    ) {
        try {
            // The screen closes immediately. Keeping this subscription on the activity
            // disposable cancels the edit in onDestroy, before Commons has stored it.
            descriptionEditHelper
                .addDescription(
                    applicationContext,
                    media,
                    updatedWikiText,
                ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { Timber.d("Descriptions are added.") },
                    { error -> Timber.e(error, "Description edit failed") },
                )
        } catch (e: InvalidLoginTokenException) {
            Timber.e(e, "Invalid login token while editing the description, logging out")
            val username: String? = sessionManager.userName
            val logoutListener =
                CommonsApplication.BaseLogoutListener(
                    this,
                    getString(R.string.invalid_login_message),
                    username,
                )

            val commonsApplication = CommonsApplication.instance
            if (commonsApplication != null) {
                commonsApplication.clearApplicationData(this, logoutListener)
            }
        }

        for (mediaDetail in uploadMediaDetails) {
            val language = mediaDetail.languageCode ?: continue
            try {
                descriptionEditHelper
                    .addCaption(
                        applicationContext,
                        media,
                        language,
                        mediaDetail.captionText,
                    ).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { Timber.d("Caption is added.") },
                        { error -> Timber.e(error, "Caption edit failed") },
                    )
            } catch (e: InvalidLoginTokenException) {
                Timber.e(e, "Invalid login token while editing a caption, logging out")
                val username = sessionManager.userName
                val logoutListener =
                    CommonsApplication.BaseLogoutListener(
                        this,
                        getString(R.string.invalid_login_message),
                        username,
                    )

                val commonsApplication = CommonsApplication.instance
                if (commonsApplication != null) {
                    commonsApplication.clearApplicationData(this, logoutListener)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelableArrayList(
            LIST_OF_DESCRIPTION_AND_CAPTION,
            uploadMediaDetailAdapter.items as ArrayList<out Parcelable?>,
        )
        outState.putString(WIKITEXT, wikiText)
        outState.putString(Prefs.DESCRIPTION_LANGUAGE, savedLanguageValue)
        // save Media
        outState.putParcelable("media", media)
    }
}
