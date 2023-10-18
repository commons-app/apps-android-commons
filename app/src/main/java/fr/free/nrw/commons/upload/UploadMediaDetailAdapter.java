package fr.free.nrw.commons.upload;

import android.app.Dialog;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.textfield.TextInputLayout;
import fr.free.nrw.commons.R;
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

public class UploadMediaDetailAdapter extends RecyclerView.Adapter<UploadMediaDetailAdapter.ViewHolder> {

    RecentLanguagesDao recentLanguagesDao;

    private List<UploadMediaDetail> uploadMediaDetails;
    private Callback callback;
    private EventListener eventListener;

    private HashMap<Integer, String> selectedLanguages;
    private final String savedLanguageValue;
    private TextView recentLanguagesTextView;
    private View separator;
    private ListView languageHistoryListView;

    public UploadMediaDetailAdapter(String savedLanguageValue, RecentLanguagesDao recentLanguagesDao) {
        uploadMediaDetails = new ArrayList<>();
        selectedLanguages = new HashMap<>();
        this.savedLanguageValue = savedLanguageValue;
        this.recentLanguagesDao = recentLanguagesDao;
    }

    public UploadMediaDetailAdapter(final String savedLanguageValue,
        List<UploadMediaDetail> uploadMediaDetails, RecentLanguagesDao recentLanguagesDao) {
        this.uploadMediaDetails = uploadMediaDetails;
        selectedLanguages = new HashMap<>();
        this.savedLanguageValue = savedLanguageValue;
        this.recentLanguagesDao = recentLanguagesDao;
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

    public List<UploadMediaDetail> getItems(){
        return uploadMediaDetails;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_item_description, parent, false));
    }

    /**
     * This is a workaround for a known bug by android here https://issuetracker.google.com/issues/37095917
     * makes the edit text on second and subsequent fragments inside an adapter receptive to long click
     * for copy/paste options
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

    /**
     * Remove description based on position from the list and notifies the RecyclerView Adapter that
     * data in adapter has been removed at that particular position.
     * @param uploadMediaDetail
     * @param position
     */
    public void removeDescription(final UploadMediaDetail uploadMediaDetail, final int position) {
        selectedLanguages.remove(position);
        final int ListPosition =
            (int) selectedLanguages.keySet().stream().filter(e -> e < position).count();
        this.uploadMediaDetails.remove(uploadMediaDetails.get(ListPosition));
        int i = position + 1;
        while (selectedLanguages.containsKey(i)) {
            selectedLanguages.remove(i);
            i++;
        }
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, uploadMediaDetails.size() - position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Nullable
        @BindView(R.id.description_languages)
        TextView descriptionLanguages;

        @BindView(R.id.description_item_edit_text)
        PasteSensitiveTextInputEditText descItemEditText;

        @BindView(R.id.description_item_edit_text_input_layout)
        TextInputLayout descInputLayout;

        @BindView(R.id.caption_item_edit_text)
        PasteSensitiveTextInputEditText captionItemEditText;

        @BindView(R.id.caption_item_edit_text_input_layout)
        TextInputLayout captionInputLayout;

        @BindView(R.id.btn_remove)
        ImageView removeButton;

        AbstractTextWatcher captionListener;

        AbstractTextWatcher descriptionListener;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            Timber.i("descItemEditText:" + descItemEditText);
        }

        public void bind(int position) {
            UploadMediaDetail uploadMediaDetail = uploadMediaDetails.get(position);
            Timber.d("UploadMediaDetail is " + uploadMediaDetail);

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

            if (position == 0) {
                removeButton.setVisibility(View.GONE);
                captionInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                captionInputLayout.setEndIconDrawable(R.drawable.maplibre_info_icon_default);
                captionInputLayout.setEndIconOnClickListener(v ->
                    callback.showAlert(R.string.media_detail_caption, R.string.caption_info));
                Objects.requireNonNull(captionInputLayout.getEditText()).setFilters(new InputFilter[] {
                    new UploadMediaDetailInputFilter()
                });

                descInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                descInputLayout.setEndIconDrawable(R.drawable.maplibre_info_icon_default);
                descInputLayout.setEndIconOnClickListener(v ->
                    callback.showAlert(R.string.media_detail_description, R.string.description_info));

            } else {
                removeButton.setVisibility(View.VISIBLE);
                captionInputLayout.setEndIconDrawable(null);
                descInputLayout.setEndIconDrawable(null);
            }

            removeButton.setOnClickListener(v -> removeDescription(uploadMediaDetail, position));
            captionListener = new AbstractTextWatcher(
                captionText -> uploadMediaDetails.get(position).setCaptionText(convertIdeographicSpaceToLatinSpace(
                    removeLeadingAndTrailingWhitespace(captionText))));
            descriptionListener = new AbstractTextWatcher(
                descriptionText -> uploadMediaDetails.get(position).setDescriptionText(descriptionText));
            captionItemEditText.addTextChangedListener(captionListener);
            initLanguage(position, uploadMediaDetail);

            descItemEditText.addTextChangedListener(descriptionListener);
            initLanguage(position, uploadMediaDetail);

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
                    dialog.getWindow().setLayout((int)(view.getContext().getResources().getDisplayMetrics().widthPixels*0.90),
                        (int)(view.getContext().getResources().getDisplayMetrics().heightPixels*0.90));
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

                    languageHistoryListView.setOnItemClickListener((adapterView, view1, position, id) -> {
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
                    if(!TextUtils.isEmpty(description.getLanguageCode())) {
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
                            description.setLanguageCode(languagesAdapter.getLanguageCode(defaultLocaleIndex));
                            selectedLanguages.remove(position);
                            selectedLanguages.put(position, languagesAdapter.getLanguageCode(defaultLocaleIndex));
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
            descriptionLanguages.setText(languageCode);
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
                    for (int i = recentLanguages.size()-1; i >=5; i--) {
                        recentLanguagesDao.deleteRecentLanguage(recentLanguages.get(i)
                            .getLanguageCode());
                    }
                }
                languageHistoryListView.setVisibility(View.VISIBLE);
                recentLanguagesTextView.setVisibility(View.VISIBLE);
                separator.setVisibility(View.VISIBLE);
                final RecentLanguagesAdapter recentLanguagesAdapter
                    = new RecentLanguagesAdapter(
                        descriptionLanguages.getContext(),
                        recentLanguagesDao.getRecentLanguages(),
                        selectedLanguages);
                languageHistoryListView.setAdapter(recentLanguagesAdapter);
            }
        }

        /**
         * Removes any leading and trailing whitespace from the source text.
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
         * @param source the source text
         * @return a string with Latin spaces instead of Ideographic spaces
         */
        public String convertIdeographicSpaceToLatinSpace(String source) {
            Pattern ideographicSpacePattern = Pattern.compile("\\x{3000}");
            return ideographicSpacePattern.matcher(source).replaceAll(" ");
        }

    }

    public interface Callback {
        void showAlert(int mediaDetailDescription, int descriptionInfo);
    }

    public interface EventListener {
        void onPrimaryCaptionTextChange(boolean isNotEmpty);
    }

}
