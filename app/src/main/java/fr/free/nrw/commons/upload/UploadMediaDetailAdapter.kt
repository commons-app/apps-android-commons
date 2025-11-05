package fr.free.nrw.commons.upload

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH
import android.speech.RecognizerIntent.EXTRA_LANGUAGE
import android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL
import android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.RowItemDescriptionBinding
import fr.free.nrw.commons.recentlanguages.Language
import fr.free.nrw.commons.recentlanguages.RecentLanguagesAdapter
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.utils.AbstractTextWatcher
import timber.log.Timber
import java.util.Locale
import java.util.regex.Pattern

class UploadMediaDetailAdapter : RecyclerView.Adapter<UploadMediaDetailAdapter.ViewHolder> {
    private var uploadMediaDetails: MutableList<UploadMediaDetail>
    private var selectedLanguages: MutableMap<Int, String>
    private val savedLanguageValue: String
    private var recentLanguagesTextView: TextView? = null
    private var separator: View? = null
    private var languageHistoryListView: ListView? = null
    private var currentPosition = 0
    private var fragment: Fragment? = null
    private var activity: Activity? = null
    private val voiceInputResultLauncher: ActivityResultLauncher<Intent>
    private var selectedVoiceIcon: SelectedVoiceIcon? = null
    var recentLanguagesDao: RecentLanguagesDao
    var callback: Callback? = null
    var eventListener: EventListener? = null
    var items: List<UploadMediaDetail>
        get() = uploadMediaDetails
        set(value) {
            uploadMediaDetails = value.toMutableList()
            selectedLanguages = mutableMapOf()
            notifyDataSetChanged()
        }


    constructor(
        fragment: Fragment?,
        savedLanguageValue: String,
        recentLanguagesDao: RecentLanguagesDao,
        voiceInputResultLauncher: ActivityResultLauncher<Intent>
    ) {
        uploadMediaDetails = ArrayList()
        selectedLanguages = mutableMapOf()
        this.savedLanguageValue = savedLanguageValue
        this.recentLanguagesDao = recentLanguagesDao
        this.fragment = fragment
        this.voiceInputResultLauncher = voiceInputResultLauncher
    }

    constructor(
        activity: Activity?,
        savedLanguageValue: String,
        uploadMediaDetails: MutableList<UploadMediaDetail>,
        recentLanguagesDao: RecentLanguagesDao,
        voiceInputResultLauncher: ActivityResultLauncher<Intent>
    ) {
        this.uploadMediaDetails = uploadMediaDetails
        selectedLanguages = HashMap()
        this.savedLanguageValue = savedLanguageValue
        this.recentLanguagesDao = recentLanguagesDao
        this.activity = activity
        this.voiceInputResultLauncher = voiceInputResultLauncher
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(RowItemDescriptionBinding.inflate(inflater, parent, false))
    }

    /**
     * This is a workaround for a known bug by android here
     * https://issuetracker.google.com/issues/37095917 makes the edit text on second and subsequent
     * fragments inside an adapter receptive to long click for copy/paste options
     *
     * @param holder the view holder
     */
    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.binding.captionItemEditText.isEnabled = false
        holder.binding.captionItemEditText.isEnabled = true
        holder.binding.descriptionItemEditText.isEnabled = false
        holder.binding.descriptionItemEditText.isEnabled = true
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return uploadMediaDetails.size
    }

    fun addDescription(uploadMediaDetail: UploadMediaDetail) {
        selectedLanguages[uploadMediaDetails.size] = "en"
        uploadMediaDetails.add(uploadMediaDetail)
        notifyItemInserted(uploadMediaDetails.size)
    }

    private fun startSpeechInput(locale: String) {
        try {
            voiceInputResultLauncher.launch(Intent(ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM)
                putExtra(EXTRA_LANGUAGE, locale)
            })
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Handles the result of the speech input by processing the spoken text.
     * If the spoken text is not empty, it capitalizes the first letter of the spoken text
     * and updates the appropriate field (caption or description) of the current
     * UploadMediaDetail based on the selected voice icon.
     * Finally, it notifies the adapter that the data set has changed.
     *
     * @param spokenText the text input received from speech recognition.
     */
    fun handleSpeechResult(spokenText: String) {
        if (spokenText.isNotEmpty()) {
            val spokenTextCapitalized =
                spokenText.substring(0, 1).uppercase(Locale.getDefault()) + spokenText.substring(1)
            if (currentPosition < uploadMediaDetails.size) {
                val uploadMediaDetail = uploadMediaDetails[currentPosition]
                when (selectedVoiceIcon) {
                    SelectedVoiceIcon.CAPTION -> uploadMediaDetail.captionText =
                        spokenTextCapitalized

                    SelectedVoiceIcon.DESCRIPTION -> uploadMediaDetail.descriptionText =
                        spokenTextCapitalized

                    null -> {}
                }
                notifyDataSetChanged()
            }
        }
    }

    /**
     * Remove description based on position from the list and notifies the RecyclerView Adapter that
     * data in adapter has been removed at that particular position.
     */
    fun removeDescription(uploadMediaDetail: UploadMediaDetail, position: Int) {
        selectedLanguages.remove(position)
        uploadMediaDetails.remove(uploadMediaDetail)
        var i = position + 1
        while (selectedLanguages.containsKey(i)) {
            selectedLanguages.remove(i)
            i++
        }
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, uploadMediaDetails.size - position)
        updateAddButtonVisibility()
    }

    fun isBonjour(): Boolean {
        if (uploadMediaDetails.size == 1) {
            val uploadMediaDetail = uploadMediaDetails[0]
            val context = (fragment?.context ?: activity) ?: return false
            val languagesAdapter = LanguagesAdapter(context, selectedLanguages)
            val defaultLocaleIndex = languagesAdapter.getIndexOfUserDefaultLocale(context)
            val defaultLanguageCode = languagesAdapter.getLanguageCode(defaultLocaleIndex)

            return uploadMediaDetail.captionText.trim().equals("bonjour", ignoreCase = true) &&
                    uploadMediaDetail.languageCode == defaultLanguageCode &&
                    uploadMediaDetail.languageCode != "fr"
        }
        return false
    }

    fun setBonjourToFrench() {
        if (uploadMediaDetails.size == 1) {
            uploadMediaDetails[0].languageCode = "fr"
            notifyItemChanged(0)
        }
    }

    inner class ViewHolder(val binding: RowItemDescriptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var addButton: ImageView? = null

        var clParent: ConstraintLayout? = null

        var betterCaptionLinearLayout: LinearLayout? = null

        var betterDescriptionLinearLayout: LinearLayout? = null

        private var captionListener: AbstractTextWatcher? = null

        var descriptionListener: AbstractTextWatcher? = null

        fun bind(position: Int) {
            val uploadMediaDetail = uploadMediaDetails[position]
            Timber.d("UploadMediaDetail is %s", uploadMediaDetail)

            addButton = binding.btnAdd
            clParent = binding.clParent
            betterCaptionLinearLayout = binding.llWriteBetterCaption
            betterDescriptionLinearLayout = binding.llWriteBetterDescription


            binding.descriptionLanguages.isFocusable = false
            binding.captionItemEditText.addTextChangedListener(AbstractTextWatcher { value: String ->
                if (position == 0) {
                    eventListener!!.onPrimaryCaptionTextChange(value.length != 0)
                }
            })
            binding.captionItemEditText.removeTextChangedListener(captionListener)
            binding.descriptionItemEditText.removeTextChangedListener(descriptionListener)
            binding.captionItemEditText.setText(uploadMediaDetail.captionText)
            binding.descriptionItemEditText.setText(uploadMediaDetail.descriptionText)
            binding.captionItemEditTextInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            binding.captionItemEditTextInputLayout.setEndIconDrawable(R.drawable.baseline_keyboard_voice)
            binding.captionItemEditTextInputLayout.setEndIconOnClickListener { v: View? ->
                currentPosition = position
                selectedVoiceIcon = SelectedVoiceIcon.CAPTION
                startSpeechInput(binding.descriptionLanguages.text.toString())
            }
            binding.descriptionItemEditTextInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            binding.descriptionItemEditTextInputLayout.setEndIconDrawable(R.drawable.baseline_keyboard_voice)
            binding.descriptionItemEditTextInputLayout.setEndIconOnClickListener { v: View? ->
                currentPosition = position
                selectedVoiceIcon = SelectedVoiceIcon.DESCRIPTION
                startSpeechInput(binding.descriptionLanguages.text.toString())
            }

            if (position == 0) {
                binding.btnRemove.visibility = View.GONE
                betterCaptionLinearLayout!!.visibility = View.VISIBLE
                betterCaptionLinearLayout!!.setOnClickListener { v: View? ->
                    callback!!.showAlert(
                        R.string.media_detail_caption,
                        R.string.caption_info
                    )
                }
                betterDescriptionLinearLayout!!.visibility = View.VISIBLE
                betterDescriptionLinearLayout!!.setOnClickListener { v: View? ->
                    callback!!.showAlert(
                        R.string.media_detail_description,
                        R.string.description_info
                    )
                }

                binding.captionItemEditTextInputLayout.editText?.let {
                    it.filters = arrayOf<InputFilter>(UploadMediaDetailInputFilter())
                }
            } else {
                binding.btnRemove.visibility = View.VISIBLE
                betterCaptionLinearLayout!!.visibility = View.GONE
                betterDescriptionLinearLayout!!.visibility = View.GONE
            }

            binding.btnRemove.setOnClickListener { v: View? ->
                removeDescription(
                    uploadMediaDetail,
                    position
                )
            }
            captionListener = AbstractTextWatcher { captionText: String ->
                uploadMediaDetail.captionText =
                    convertIdeographicSpaceToLatinSpace(captionText.trim())
            }
            descriptionListener = AbstractTextWatcher { value: String? ->
                uploadMediaDetail.descriptionText = value
            }
            binding.captionItemEditText.addTextChangedListener(captionListener)
            initLanguage(position, uploadMediaDetail)

            binding.descriptionItemEditText.addTextChangedListener(descriptionListener)
            initLanguage(position, uploadMediaDetail)

            if (fragment != null) {
                val newLayoutParams = clParent!!.layoutParams as FrameLayout.LayoutParams
                newLayoutParams.topMargin = 0
                newLayoutParams.leftMargin = 0
                newLayoutParams.rightMargin = 0
                newLayoutParams.bottomMargin = 0
                clParent!!.layoutParams = newLayoutParams
            }
            updateAddButtonVisibility()
            addButton!!.setOnClickListener { v: View? -> eventListener!!.addLanguage() }

            //If the description was manually added by the user, it deserves focus, if not, let the user decide
            if (uploadMediaDetail.isManuallyAdded) {
                binding.captionItemEditText.requestFocus()
            } else {
                binding.captionItemEditText.clearFocus()
            }
        }


        private fun initLanguage(position: Int, description: UploadMediaDetail) {
            val recentLanguages = recentLanguagesDao.getRecentLanguages()

            val languagesAdapter = LanguagesAdapter(
                binding.descriptionLanguages.context,
                selectedLanguages
            )

            binding.descriptionLanguages.setOnClickListener { view ->
                val dialog = Dialog(view.context)
                dialog.setContentView(R.layout.dialog_select_language)
                dialog.setCancelable(false)
                dialog.window!!.setLayout(
                    (view.context.resources.displayMetrics.widthPixels
                            * 0.90).toInt(),
                    (view.context.resources.displayMetrics.heightPixels
                            * 0.90).toInt()
                )
                dialog.show()

                val editText =
                    dialog.findViewById<EditText>(R.id.search_language)
                val listView =
                    dialog.findViewById<ListView>(R.id.language_list)
                languageHistoryListView =
                    dialog.findViewById(R.id.language_history_list)
                recentLanguagesTextView =
                    dialog.findViewById(R.id.recent_searches)
                separator =
                    dialog.findViewById(R.id.separator)
                setUpRecentLanguagesSection(recentLanguages)

                listView.adapter = languagesAdapter

                dialog.findViewById<Button>(R.id.cancel_button)
                    .setOnClickListener { v: View? -> dialog.dismiss() }

                editText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) =
                        hideRecentLanguagesSection()

                    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                        languagesAdapter.filter.filter(charSequence)
                    }

                    override fun afterTextChanged(editable: Editable) = Unit
                })

                languageHistoryListView?.setOnItemClickListener { adapterView: AdapterView<*>, view1: View?, position: Int, id: Long ->
                    onRecentLanguageClicked(dialog, adapterView, position, description)
                }

                listView.onItemClickListener = OnItemClickListener { adapterView, _, i, l ->
                        description.selectedLanguageIndex = i
                        val languageCode = (adapterView.adapter as LanguagesAdapter).getLanguageCode(i)
                        description.languageCode = languageCode
                        val languageName = (adapterView.adapter as LanguagesAdapter).getLanguageName(i)
                        val isExists = recentLanguagesDao.findRecentLanguage(languageCode)
                        if (isExists) {
                            recentLanguagesDao.deleteRecentLanguage(languageCode)
                        }
                        recentLanguagesDao.addRecentLanguage(Language(languageName, languageCode))

                        selectedLanguages.clear()
                        selectedLanguages[position] = languageCode
                        (adapterView.adapter as LanguagesAdapter).selectedLangCode = languageCode
                        Timber.d("Description language code is: %s", languageCode)
                    binding.descriptionLanguages.text = languageCode
                        dialog.dismiss()
                    }

                dialog.setOnDismissListener {
                    languagesAdapter.filter.filter("")
                }
            }

            if (description.selectedLanguageIndex == -1) {
                if (!TextUtils.isEmpty(savedLanguageValue)) {
                    // If user has chosen a default language from settings activity
                    // savedLanguageValue is not null
                    if (!TextUtils.isEmpty(description.languageCode)) {
                        binding.descriptionLanguages.text = description.languageCode
                        selectedLanguages.remove(position)
                        selectedLanguages[position] = description.languageCode!!
                    } else {
                        description.languageCode = savedLanguageValue
                        binding.descriptionLanguages.text = savedLanguageValue
                        selectedLanguages.remove(position)
                        selectedLanguages[position] = savedLanguageValue
                    }
                } else if (!TextUtils.isEmpty(description.languageCode)) {
                    binding.descriptionLanguages.text = description.languageCode
                    selectedLanguages.remove(position)
                    selectedLanguages[position] = description.languageCode!!
                } else {
                    //Checking whether Language Code attribute is null or not.
                    if (uploadMediaDetails[position].languageCode != null) {
                        //If it is not null that means it is fetching details from the previous
                        // upload (i.e. when user has pressed copy previous caption & description)
                        //hence providing same language code for the current upload.
                        binding.descriptionLanguages.text = uploadMediaDetails[position]
                            .languageCode
                        selectedLanguages.remove(position)
                        selectedLanguages[position] = uploadMediaDetails[position].languageCode!!
                    } else {
                        if (position == 0) {
                            val defaultLocaleIndex = languagesAdapter.getIndexOfUserDefaultLocale(
                                binding.descriptionLanguages.getContext())
                            binding.descriptionLanguages.setText(languagesAdapter.getLanguageCode(defaultLocaleIndex))
                            description.languageCode = languagesAdapter.getLanguageCode(defaultLocaleIndex)
                            selectedLanguages.remove(position)
                            selectedLanguages[position] =
                                languagesAdapter.getLanguageCode(defaultLocaleIndex)
                        } else {
                            description.languageCode = languagesAdapter.getLanguageCode(0)
                            binding.descriptionLanguages.text = languagesAdapter.getLanguageCode(0)
                            selectedLanguages.remove(position)
                            selectedLanguages[position] = languagesAdapter.getLanguageCode(0)
                        }
                    }
                }
            } else {
                binding.descriptionLanguages.text = description.languageCode
                selectedLanguages.remove(position)
                description.languageCode?.let {
                    selectedLanguages[position] = it
                }
            }
        }

        /**
         * Handles click event for recent language section
         */
        private fun onRecentLanguageClicked(
            dialog: Dialog, adapterView: AdapterView<*>,
            position: Int, description: UploadMediaDetail
        ) {
            description.selectedLanguageIndex = position
            val languageCode = (adapterView.adapter as RecentLanguagesAdapter)
                .getLanguageCode(position)
            description.languageCode = languageCode
            val languageName = (adapterView.adapter as RecentLanguagesAdapter)
                .getLanguageName(position)
            val isExists = recentLanguagesDao.findRecentLanguage(languageCode)
            if (isExists) {
                recentLanguagesDao.deleteRecentLanguage(languageCode)
            }
            recentLanguagesDao.addRecentLanguage(Language(languageName, languageCode))

            selectedLanguages.clear()
            selectedLanguages[position] = languageCode
            (adapterView
                .adapter as RecentLanguagesAdapter).selectedLangCode = languageCode
            Timber.d("Description language code is: %s", languageCode)
            binding.descriptionLanguages.text = languageCode
            dialog.dismiss()
        }

        /**
         * Hides recent languages section
         */
        private fun hideRecentLanguagesSection() {
            languageHistoryListView!!.visibility = View.GONE
            recentLanguagesTextView!!.visibility = View.GONE
            separator!!.visibility = View.GONE
        }

        /**
         * Set up recent languages section
         *
         * @param recentLanguages recently used languages
         */
        private fun setUpRecentLanguagesSection(recentLanguages: List<Language>) {
            if (recentLanguages.isEmpty()) {
                languageHistoryListView!!.visibility = View.GONE
                recentLanguagesTextView!!.visibility = View.GONE
                separator!!.visibility = View.GONE
            } else {
                if (recentLanguages.size > 5) {
                    for (i in recentLanguages.size - 1 downTo 5) {
                        recentLanguagesDao.deleteRecentLanguage(
                            recentLanguages[i]
                                .languageCode
                        )
                    }
                }
                languageHistoryListView!!.visibility = View.VISIBLE
                recentLanguagesTextView!!.visibility = View.VISIBLE
                separator!!.visibility = View.VISIBLE

                val recentLanguagesAdapter = RecentLanguagesAdapter(
                    binding.descriptionLanguages.context,
                    recentLanguagesDao.getRecentLanguages(),
                    selectedLanguages
                )
                languageHistoryListView!!.adapter = recentLanguagesAdapter
            }
        }

        /**
         * Convert Ideographic space to Latin space
         *
         * @param source the source text
         * @return a string with Latin spaces instead of Ideographic spaces
         */
        fun convertIdeographicSpaceToLatinSpace(source: String): String {
            val ideographicSpacePattern = Pattern.compile("\\x{3000}")
            return ideographicSpacePattern.matcher(source).replaceAll(" ")
        }
    }

    /**
     * Hides the visibility of the "Add" button for all items in the RecyclerView except
     * the last item in RecyclerView
     */
    private fun updateAddButtonVisibility() {
        val lastItemPosition = itemCount - 1
        // Hide Add Button for all items
        for (i in 0 until itemCount) {
            if (fragment != null) {
                if (fragment!!.view != null) {
                    val holder = (fragment!!.requireView().findViewById<View>(R.id.rv_descriptions) as RecyclerView).findViewHolderForAdapterPosition(i) as ViewHolder?
                    if (holder != null) {
                        holder.addButton!!.visibility = View.GONE
                    }
                }
            } else {
                if (activity != null) {
                    val holder = (activity!!.findViewById<View>(R.id.rv_descriptions_captions) as RecyclerView).findViewHolderForAdapterPosition(i) as ViewHolder?
                    if (holder != null) {
                        holder.addButton!!.visibility = View.GONE
                    }
                }
            }
        }

        // Show Add Button for the last item
        if (fragment != null) {
            if (fragment!!.view != null) {
                val lastItemHolder = (fragment!!.requireView().findViewById<View>(R.id.rv_descriptions) as RecyclerView).findViewHolderForAdapterPosition(lastItemPosition) as ViewHolder?
                if (lastItemHolder != null) {
                    lastItemHolder.addButton!!.visibility = View.VISIBLE
                }
            }
        } else {
            if (activity != null) {
                val lastItemHolder = (activity!!.findViewById<View>(R.id.rv_descriptions_captions) as RecyclerView).findViewHolderForAdapterPosition(lastItemPosition) as ViewHolder?
                if (lastItemHolder != null) {
                    lastItemHolder.addButton!!.visibility = View.VISIBLE
                }
            }
        }
    }

    fun interface Callback {
        fun showAlert(mediaDetailDescription: Int, descriptionInfo: Int)
    }

    interface EventListener {
        fun onPrimaryCaptionTextChange(isNotEmpty: Boolean)
        fun addLanguage()
    }

    internal enum class SelectedVoiceIcon {
        CAPTION,
        DESCRIPTION
    }
}
