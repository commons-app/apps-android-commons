package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputLayout;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.databinding.RowItemDescriptionBinding;
import fr.free.nrw.commons.recentlanguages.Language;
import fr.free.nrw.commons.recentlanguages.RecentLanguagesAdapter;
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao;
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText;
import fr.free.nrw.commons.utils.AbstractTextWatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import timber.log.Timber;

public class UploadMediaDetailAdapter extends
    RecyclerView.Adapter<UploadMediaDetailAdapter.ViewHolder> {

    RecentLanguagesDao recentLanguagesDao;

    private List<UploadMediaDetail> uploadMediaDetails;
    private Callback callback;
    private EventListener eventListener;

    private HashMap<Integer, String> selectedLanguages;
    private final String savedLanguageValue;
    private TextView recentLanguagesTextView;
    private View separator;
    private ListView languageHistoryListView;
    private int currentPosition;
    private Fragment fragment;
    private Activity activity;
    private SelectedVoiceIcon selectedVoiceIcon;
    private static final int REQUEST_CODE_FOR_VOICE_INPUT = 1213;

    private RowItemDescriptionBinding binding;

    public UploadMediaDetailAdapter(Fragment fragment, String savedLanguageValue,
        RecentLanguagesDao recentLanguagesDao) {
        uploadMediaDetails = new ArrayList<>();
        selectedLanguages = new HashMap<>();
        this.savedLanguageValue = savedLanguageValue;
        this.recentLanguagesDao = recentLanguagesDao;
        this.fragment = fragment;
    }

    public UploadMediaDetailAdapter(Activity activity, final String savedLanguageValue,
        List<UploadMediaDetail> uploadMediaDetails, RecentLanguagesDao recentLanguagesDao) {
        this.uploadMediaDetails = uploadMediaDetails;
        selectedLanguages = new HashMap<>();
        this.savedLanguageValue = savedLanguageValue;
        this.recentLanguagesDao = recentLanguagesDao;
        this.activity = activity;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setItems(List<UploadMediaDetail> uploadMediaDetails) {
        this.uploadMediaDetails = uploadMediaDetails;
        selectedLanguages = new HashMap<>();
        notifyDataSetChanged();
    }

    public List<UploadMediaDetail> getItems() {
        return uploadMediaDetails;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        binding = RowItemDescriptionBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding.getRoot());
    }

    /**
     * This is a workaround for a known bug by android here
     * https://issuetracker.google.com/issues/37095917 makes the edit text on second and subsequent
     * fragments inside an adapter receptive to long click for copy/paste options
     *
     * @param holder the view holder
     */
    @Override
    public void onViewAttachedToWindow(@NonNull final ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.captionItemEditText.setEnabled(false);
        holder.captionItemEditText.setEnabled(true);
        holder.descItemEditText.setEnabled(false);
        holder.descItemEditText.setEnabled(true);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return uploadMediaDetails.size();
    }

    public void addDescription(UploadMediaDetail uploadMediaDetail) {
        selectedLanguages.put(uploadMediaDetails.size(), "en");
        this.uploadMediaDetails.add(uploadMediaDetail);
        notifyItemInserted(uploadMediaDetails.size());
    }

    private void startSpeechInput(String locale) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            locale
        );

        try {
            if (activity == null) {
                fragment.startActivityForResult(intent, REQUEST_CODE_FOR_VOICE_INPUT);
            } else {
                activity.startActivityForResult(intent, REQUEST_CODE_FOR_VOICE_INPUT);
            }
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }
    }

    public void handleSpeechResult(String spokenText) {
        if (!spokenText.isEmpty()) {
            String spokenTextCapitalized =
                spokenText.substring(0, 1).toUpperCase() + spokenText.substring(1);
            if (currentPosition < uploadMediaDetails.size()) {
                UploadMediaDetail uploadMediaDetail = uploadMediaDetails.get(currentPosition);
                if (selectedVoiceIcon == SelectedVoiceIcon.CAPTION) {
                    uploadMediaDetail.setCaptionText(spokenTextCapitalized);
                } else {
                    uploadMediaDetail.setDescriptionText(spokenTextCapitalized);
                }
                notifyItemChanged(currentPosition);
            }
        }
    }

    /**
     * Remove description based on position from the list and notifies the RecyclerView Adapter that
     * data in adapter has been removed at that particular position.
     *
     * @param uploadMediaDetail
     * @param position
     */
    public void removeDescription(final UploadMediaDetail uploadMediaDetail, final int position) {
        selectedLanguages.remove(position);
        int listPosition = 0;
        List<Integer> keysList = new ArrayList<>(selectedLanguages.keySet());
        for (Integer key : keysList) {
            if (key < position) {
                listPosition++;
            }
        }
        this.uploadMediaDetails.remove(uploadMediaDetails.get(listPosition));
        int i = position + 1;
        while (selectedLanguages.containsKey(i)) {
            selectedLanguages.remove(i);
            i++;
        }
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, uploadMediaDetails.size() - position);
        updateAddButtonVisibility();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView descriptionLanguages ;

        PasteSensitiveTextInputEditText descItemEditText;

        TextInputLayout descInputLayout;

        PasteSensitiveTextInputEditText captionItemEditText;

        TextInputLayout captionInputLayout;

        ImageView removeButton;

        ImageView addButton;

        ConstraintLayout clParent;

        LinearLayout betterCaptionLinearLayout;

        LinearLayout betterDescriptionLinearLayout;

        private

        AbstractTextWatcher captionListener;

        AbstractTextWatcher descriptionListener;

        public ViewHolder(View itemView) {
            super(itemView);
            Timber.i("descItemEditText:" + descItemEditText);
        }

        public void bind(int position) {
            UploadMediaDetail uploadMediaDetail = uploadMediaDetails.get(position);
            Timber.d("UploadMediaDetail is " + uploadMediaDetail);

            descriptionLanguages = binding.descriptionLanguages;
            descItemEditText = binding.descriptionItemEditText;
            descInputLayout = binding.descriptionItemEditTextInputLayout;
            captionItemEditText = binding.captionItemEditText;
            captionInputLayout = binding.captionItemEditTextInputLayout;
            removeButton = binding.btnRemove;
            addButton = binding.btnAdd;
            clParent = binding.clParent;
            betterCaptionLinearLayout = binding.llWriteBetterCaption;
            betterDescriptionLinearLayout = binding.llWriteBetterDescription;


            descriptionLanguages.setFocusable(false);
            captionItemEditText.addTextChangedListener(new AbstractTextWatcher(
                value -> {
                    if (position == 0) {
                        eventListener.onPrimaryCaptionTextChange(value.length() != 0);
                    }
                }));
            captionItemEditText.removeTextChangedListener(captionListener);
            descItemEditText.removeTextChangedListener(descriptionListener);
            captionItemEditText.setText(uploadMediaDetail.getCaptionText());
            descItemEditText.setText(uploadMediaDetail.getDescriptionText());
            captionInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            captionInputLayout.setEndIconDrawable(R.drawable.baseline_keyboard_voice);
            captionInputLayout.setEndIconOnClickListener(v -> {
                currentPosition = position;
                selectedVoiceIcon = SelectedVoiceIcon.CAPTION;
                startSpeechInput(descriptionLanguages.getText().toString());
            });
            descInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            descInputLayout.setEndIconDrawable(R.drawable.baseline_keyboard_voice);
            descInputLayout.setEndIconOnClickListener(v -> {
                currentPosition = position;
                selectedVoiceIcon = SelectedVoiceIcon.DESCRIPTION;
                startSpeechInput(descriptionLanguages.getText().toString());
            });

            if (position == 0) {
                removeButton.setVisibility(View.GONE);
                betterCaptionLinearLayout.setVisibility(View.VISIBLE);
                betterCaptionLinearLayout.setOnClickListener(
                    v -> callback.showAlert(R.string.media_detail_caption, R.string.caption_info));
                betterDescriptionLinearLayout.setVisibility(View.VISIBLE);
                betterDescriptionLinearLayout.setOnClickListener(
                    v -> callback.showAlert(R.string.media_detail_description,
                        R.string.description_info));
                Objects.requireNonNull(captionInputLayout.getEditText())
                    .setFilters(new InputFilter[]{
                        new UploadMediaDetailInputFilter()
                    });
            } else {
                removeButton.setVisibility(View.VISIBLE);
                betterCaptionLinearLayout.setVisibility(View.GONE);
                betterDescriptionLinearLayout.setVisibility(View.GONE);
            }

            removeButton.setOnClickListener(v -> removeDescription(uploadMediaDetail, position));
            captionListener = new AbstractTextWatcher(
                captionText -> uploadMediaDetails.get(position)
                    .setCaptionText(convertIdeographicSpaceToLatinSpace(
                        removeLeadingAndTrailingWhitespace(captionText))));
            descriptionListener = new AbstractTextWatcher(
                descriptionText -> uploadMediaDetails.get(position)
                    .setDescriptionText(descriptionText));
            captionItemEditText.addTextChangedListener(captionListener);
            initLanguage(position, uploadMediaDetail);

            descItemEditText.addTextChangedListener(descriptionListener);
            initLanguage(position, uploadMediaDetail);

            if (fragment != null) {
                FrameLayout.LayoutParams newLayoutParams = (FrameLayout.LayoutParams) clParent.getLayoutParams();
                newLayoutParams.topMargin = 0;
                newLayoutParams.leftMargin = 0;
                newLayoutParams.rightMargin = 0;
                newLayoutParams.bottomMargin = 0;
                clParent.setLayoutParams(newLayoutParams);
            }
            updateAddButtonVisibility();
            addButton.setOnClickListener(v -> eventListener.addLanguage());

            //If the description was manually added by the user, it deserves focus, if not, let the user decide
            if (uploadMediaDetail.isManuallyAdded()) {
                captionItemEditText.requestFocus();
            } else {
                captionItemEditText.clearFocus();
            }
        }


        private void initLanguage(int position, UploadMediaDetail description) {

            final List<Language> recentLanguages = recentLanguagesDao.getRecentLanguages();

            LanguagesAdapter languagesAdapter = new LanguagesAdapter(
                descriptionLanguages.getContext(),
                selectedLanguages
            );

            descriptionLanguages.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Dialog dialog = new Dialog(view.getContext());
                    dialog.setContentView(R.layout.dialog_select_language);
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.getWindow().setLayout(
                        (int) (view.getContext().getResources().getDisplayMetrics().widthPixels
                            * 0.90),
                        (int) (view.getContext().getResources().getDisplayMetrics().heightPixels
                            * 0.90));
                    dialog.show();

                    EditText editText = dialog.findViewById(R.id.search_language);
                    ListView listView = dialog.findViewById(R.id.language_list);
                    languageHistoryListView = dialog.findViewById(R.id.language_history_list);
                    recentLanguagesTextView = dialog.findViewById(R.id.recent_searches);
                    separator = dialog.findViewById(R.id.separator);
                    setUpRecentLanguagesSection(recentLanguages);

                    listView.setAdapter(languagesAdapter);

                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                            int i2) {
                            hideRecentLanguagesSection();
                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1,
                            int i2) {
                            languagesAdapter.getFilter().filter(charSequence);
                        }

                        @Override
                        public void afterTextChanged(Editable editable) {

                        }
                    });

                    languageHistoryListView.setOnItemClickListener(
                        (adapterView, view1, position, id) -> {
                            onRecentLanguageClicked(dialog, adapterView, position, description);
                        });

                    listView.setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i,
                            long l) {
                            description.setSelectedLanguageIndex(i);
                            String languageCode = ((LanguagesAdapter) adapterView.getAdapter())
                                .getLanguageCode(i);
                            description.setLanguageCode(languageCode);
                            final String languageName
                                = ((LanguagesAdapter) adapterView.getAdapter()).getLanguageName(i);
                            final boolean isExists
                                = recentLanguagesDao.findRecentLanguage(languageCode);
                            if (isExists) {
                                recentLanguagesDao.deleteRecentLanguage(languageCode);
                            }
                            recentLanguagesDao
                                .addRecentLanguage(new Language(languageName, languageCode));

                            selectedLanguages.remove(position);
                            selectedLanguages.put(position, languageCode);
                            ((LanguagesAdapter) adapterView
                                .getAdapter()).setSelectedLangCode(languageCode);
                            Timber.d("Description language code is: " + languageCode);
                            descriptionLanguages.setText(languageCode);
                            dialog.dismiss();
                        }
                    });

                    dialog.setOnDismissListener(
                        dialogInterface -> languagesAdapter.getFilter().filter(""));

                }
            });

            if (description.getSelectedLanguageIndex() == -1) {
                if (!TextUtils.isEmpty(savedLanguageValue)) {
                    // If user has chosen a default language from settings activity
                    // savedLanguageValue is not null
                    if (!TextUtils.isEmpty(description.getLanguageCode())) {
                        descriptionLanguages.setText(description.getLanguageCode());
                        selectedLanguages.remove(position);
                        selectedLanguages.put(position, description.getLanguageCode());
                    } else {
                        description.setLanguageCode(savedLanguageValue);
                        descriptionLanguages.setText(savedLanguageValue);
                        selectedLanguages.remove(position);
                        selectedLanguages.put(position, savedLanguageValue);
                    }
                } else if (!TextUtils.isEmpty(description.getLanguageCode())) {
                    descriptionLanguages.setText(description.getLanguageCode());
                    selectedLanguages.remove(position);
                    selectedLanguages.put(position, description.getLanguageCode());
                } else {
                    //Checking whether Language Code attribute is null or not.
                    if (uploadMediaDetails.get(position).getLanguageCode() != null) {
                        //If it is not null that means it is fetching details from the previous
                        // upload (i.e. when user has pressed copy previous caption & description)
                        //hence providing same language code for the current upload.
                        descriptionLanguages.setText(uploadMediaDetails.get(position)
                            .getLanguageCode());
                        selectedLanguages.remove(position);
                        selectedLanguages.put(position, uploadMediaDetails.get(position)
                            .getLanguageCode());
                    } else {
                        if (position == 0) {
                            final int defaultLocaleIndex = languagesAdapter
                                .getIndexOfUserDefaultLocale(descriptionLanguages
                                    .getContext());
                            descriptionLanguages
                                .setText(languagesAdapter.getLanguageCode(defaultLocaleIndex));
                            description.setLanguageCode(
                                languagesAdapter.getLanguageCode(defaultLocaleIndex));
                            selectedLanguages.remove(position);
                            selectedLanguages.put(position,
                                languagesAdapter.getLanguageCode(defaultLocaleIndex));
                        } else {
                            description.setLanguageCode(languagesAdapter.getLanguageCode(0));
                            descriptionLanguages.setText(languagesAdapter.getLanguageCode(0));
                            selectedLanguages.remove(position);
                            selectedLanguages.put(position, languagesAdapter.getLanguageCode(0));
                        }
                    }
                }

            } else {
                descriptionLanguages.setText(description.getLanguageCode());
                selectedLanguages.remove(position);
                selectedLanguages.put(position, description.getLanguageCode());
            }
        }

        /**
         * Handles click event for recent language section
         */
        private void onRecentLanguageClicked(final Dialog dialog, final AdapterView<?> adapterView,
            final int position, final UploadMediaDetail description) {
            description.setSelectedLanguageIndex(position);
            final String languageCode = ((RecentLanguagesAdapter) adapterView.getAdapter())
                .getLanguageCode(position);
            description.setLanguageCode(languageCode);
            final String languageName = ((RecentLanguagesAdapter) adapterView.getAdapter())
                .getLanguageName(position);
            final boolean isExists = recentLanguagesDao.findRecentLanguage(languageCode);
            if (isExists) {
                recentLanguagesDao.deleteRecentLanguage(languageCode);
            }
            recentLanguagesDao.addRecentLanguage(new Language(languageName, languageCode));

            selectedLanguages.remove(position);
            selectedLanguages.put(position, languageCode);
            ((RecentLanguagesAdapter) adapterView
                .getAdapter()).setSelectedLangCode(languageCode);
            Timber.d("Description language code is: %s", languageCode);
            if (descriptionLanguages!=null) {
                descriptionLanguages.setText(languageCode);
            }
            dialog.dismiss();
        }

        /**
         * Hides recent languages section
         */
        private void hideRecentLanguagesSection() {
            languageHistoryListView.setVisibility(View.GONE);
            recentLanguagesTextView.setVisibility(View.GONE);
            separator.setVisibility(View.GONE);
        }

        /**
         * Set up recent languages section
         *
         * @param recentLanguages recently used languages
         */
        private void setUpRecentLanguagesSection(final List<Language> recentLanguages) {
            if (recentLanguages.isEmpty()) {
                languageHistoryListView.setVisibility(View.GONE);
                recentLanguagesTextView.setVisibility(View.GONE);
                separator.setVisibility(View.GONE);
            } else {
                if (recentLanguages.size() > 5) {
                    for (int i = recentLanguages.size() - 1; i >= 5; i--) {
                        recentLanguagesDao.deleteRecentLanguage(recentLanguages.get(i)
                            .getLanguageCode());
                    }
                }
                languageHistoryListView.setVisibility(View.VISIBLE);
                recentLanguagesTextView.setVisibility(View.VISIBLE);
                separator.setVisibility(View.VISIBLE);

                if (descriptionLanguages!=null) {
                    final RecentLanguagesAdapter recentLanguagesAdapter
                        = new RecentLanguagesAdapter(
                        descriptionLanguages.getContext(),
                        recentLanguagesDao.getRecentLanguages(),
                        selectedLanguages);
                    languageHistoryListView.setAdapter(recentLanguagesAdapter);
                }
            }
        }

        /**
         * Removes any leading and trailing whitespace from the source text.
         *
         * @param source input string
         * @return a string without leading and trailing whitespace
         */
        public String removeLeadingAndTrailingWhitespace(String source) {
            // This method can be replaced with the inbuilt String::strip when updated to JDK 11.
            // Note that String::trim does not adequately remove all whitespace chars.
            int firstNonWhitespaceIndex = 0;
            while (firstNonWhitespaceIndex < source.length()) {
                if (Character.isWhitespace(source.charAt(firstNonWhitespaceIndex))) {
                    firstNonWhitespaceIndex++;
                } else {
                    break;
                }
            }
            if (firstNonWhitespaceIndex == source.length()) {
                return "";
            }

            int lastNonWhitespaceIndex = source.length() - 1;
            while (lastNonWhitespaceIndex > firstNonWhitespaceIndex) {
                if (Character.isWhitespace(source.charAt(lastNonWhitespaceIndex))) {
                    lastNonWhitespaceIndex--;
                } else {
                    break;
                }
            }

            return source.substring(firstNonWhitespaceIndex, lastNonWhitespaceIndex + 1);
        }

        /**
         * Convert Ideographic space to Latin space
         *
         * @param source the source text
         * @return a string with Latin spaces instead of Ideographic spaces
         */
        public String convertIdeographicSpaceToLatinSpace(String source) {
            Pattern ideographicSpacePattern = Pattern.compile("\\x{3000}");
            return ideographicSpacePattern.matcher(source).replaceAll(" ");
        }

    }

    /**
     * Hides the visibility of the "Add" button for all items in the RecyclerView except
     * the last item in RecyclerView
     */
    private void updateAddButtonVisibility() {
        int lastItemPosition = getItemCount() - 1;
        // Hide Add Button for all items
        for (int i = 0; i < getItemCount(); i++) {
            if (fragment != null) {
                if (fragment.getView() != null) {
                    ViewHolder holder = (ViewHolder) ((RecyclerView) fragment.getView()
                        .findViewById(R.id.rv_descriptions)).findViewHolderForAdapterPosition(i);
                    if (holder != null) {
                        holder.addButton.setVisibility(View.GONE);
                    }
                }
            } else {
                if (this.activity != null) {
                    ViewHolder holder = (ViewHolder) ((RecyclerView) activity.findViewById(
                        R.id.rv_descriptions_captions)).findViewHolderForAdapterPosition(i);
                    if (holder != null) {
                        holder.addButton.setVisibility(View.GONE);
                    }
                }
            }
        }

        // Show Add Button for the last item
        if (fragment != null) {
            if (fragment.getView() != null) {
                ViewHolder lastItemHolder = (ViewHolder) ((RecyclerView) fragment.getView()
                    .findViewById(R.id.rv_descriptions)).findViewHolderForAdapterPosition(
                    lastItemPosition);
                if (lastItemHolder != null) {
                    lastItemHolder.addButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            if (this.activity != null) {
                ViewHolder lastItemHolder = (ViewHolder) ((RecyclerView) activity
                    .findViewById(R.id.rv_descriptions_captions)).findViewHolderForAdapterPosition(
                    lastItemPosition);
                if (lastItemHolder != null) {
                    lastItemHolder.addButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public interface Callback {

        void showAlert(int mediaDetailDescription, int descriptionInfo);
    }

    public interface EventListener {

        void onPrimaryCaptionTextChange(boolean isNotEmpty);

        void addLanguage();
    }

    enum SelectedVoiceIcon {
        CAPTION,
        DESCRIPTION
    }
}
