package fr.free.nrw.commons.upload;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.textfield.TextInputLayout;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.AbstractTextWatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import timber.log.Timber;

public class UploadMediaDetailAdapter extends RecyclerView.Adapter<UploadMediaDetailAdapter.ViewHolder> {

    private List<UploadMediaDetail> uploadMediaDetails;
    private Callback callback;
    private EventListener eventListener;

    private HashMap<AdapterView, String> selectedLanguages;
    private final String savedLanguageValue;

    public UploadMediaDetailAdapter(String savedLanguageValue) {
        uploadMediaDetails = new ArrayList<>();
        selectedLanguages = new HashMap<>();
        this.savedLanguageValue = savedLanguageValue;
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_item_description, parent, false));
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
        this.uploadMediaDetails.add(uploadMediaDetail);
        notifyItemInserted(uploadMediaDetails.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Nullable
        @BindView(R.id.spinner_description_languages)
        AppCompatSpinner spinnerDescriptionLanguages;

        @BindView(R.id.description_item_edit_text)
        AppCompatEditText descItemEditText;

        @BindView(R.id.description_item_edit_text_input_layout)
        TextInputLayout descInputLayout;

        @BindView(R.id.caption_item_edit_text)
        AppCompatEditText captionItemEditText;

        @BindView(R.id.caption_item_edit_text_input_layout)
        TextInputLayout captionInputLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            Timber.i("descItemEditText:" + descItemEditText);
        }

        public void bind(int position) {
            UploadMediaDetail uploadMediaDetail = uploadMediaDetails.get(position);
            Timber.d("UploadMediaDetail is " + uploadMediaDetail);

            captionItemEditText.addTextChangedListener(new AbstractTextWatcher(
                value -> {
                    if (position == 0) {
                        eventListener.onPrimaryCaptionTextChange(value.length() != 0);
                    }
                }));
            captionItemEditText.setText(uploadMediaDetail.getCaptionText());
            descItemEditText.setText(uploadMediaDetail.getDescriptionText());

            if (position == 0) {
                captionInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                captionInputLayout.setEndIconDrawable(R.drawable.mapbox_info_icon_default);
                captionInputLayout.setEndIconOnClickListener(v ->
                    callback.showAlert(R.string.media_detail_caption, R.string.caption_info));

                descInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                descInputLayout.setEndIconDrawable(R.drawable.mapbox_info_icon_default);
                descInputLayout.setEndIconOnClickListener(v ->
                    callback.showAlert(R.string.media_detail_description, R.string.description_info));

            } else {
                captionInputLayout.setEndIconDrawable(null);
                descInputLayout.setEndIconDrawable(null);
            }

            captionItemEditText.addTextChangedListener(new AbstractTextWatcher(
                    captionText -> uploadMediaDetails.get(position).setCaptionText(captionText)));
            initLanguageSpinner(position, uploadMediaDetail);

            descItemEditText.addTextChangedListener(new AbstractTextWatcher(
                    descriptionText -> uploadMediaDetails.get(position).setDescriptionText(descriptionText)));
            initLanguageSpinner(position, uploadMediaDetail);

            //If the description was manually added by the user, it deserves focus, if not, let the user decide
            if (uploadMediaDetail.isManuallyAdded()) {
                captionItemEditText.requestFocus();
            } else {
                captionItemEditText.clearFocus();
            }
        }

        /**
         * Extracted out the function to init the language spinner with different system supported languages
         * @param position
         * @param description
         */
        private void initLanguageSpinner(int position, UploadMediaDetail description) {
            SpinnerLanguagesAdapter languagesAdapter = new SpinnerLanguagesAdapter(
                    spinnerDescriptionLanguages.getContext(),
                    selectedLanguages
            );
            spinnerDescriptionLanguages.setAdapter(languagesAdapter);

            spinnerDescriptionLanguages.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                                           long l) {
                    description.setSelectedLanguageIndex(position);
                    String languageCode = ((SpinnerLanguagesAdapter) adapterView.getAdapter())
                            .getLanguageCode(position);
                    description.setLanguageCode(languageCode);
                    selectedLanguages.remove(adapterView);
                    selectedLanguages.put(adapterView, languageCode);
                    ((SpinnerLanguagesAdapter) adapterView
                            .getAdapter()).setSelectedLangCode(languageCode);
                    spinnerDescriptionLanguages.setSelection(position);
                    Timber.d("Description language code is: "+languageCode);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });


            if(description.getCaptionText().isEmpty() == false
                && languagesAdapter.getIndexOfLanguageCode(description.getLanguageCode()) != -1){
                // If the user selects a nearby pin or location bookmark to upload a picture and language is present in spinner we set the language.
                spinnerDescriptionLanguages.setSelection(languagesAdapter.getIndexOfLanguageCode(description.getLanguageCode()));
            }
            else {
                // This is a contribution upload or the language from description is not present in spinner.
                if (description.getSelectedLanguageIndex() == -1) {
                    if (!TextUtils.isEmpty(savedLanguageValue)) {
                        // If user has chosen a default language from settings activity savedLanguageValue is not null
                        spinnerDescriptionLanguages
                            .setSelection(
                                languagesAdapter.getIndexOfLanguageCode(savedLanguageValue));
                    } else {
                        //Checking whether Language Code attribute is null or not.
                        if (uploadMediaDetails.get(position).getLanguageCode() != null) {
                            //If it is not null that means it is fetching details from the previous upload (i.e. when user has pressed copy previous caption & description)
                            //hence providing same language code for the current upload.
                            spinnerDescriptionLanguages.setSelection(languagesAdapter
                                .getIndexOfLanguageCode(
                                    uploadMediaDetails.get(position).getLanguageCode()), true);
                        } else {
                            if (position == 0) {
                                int defaultLocaleIndex = languagesAdapter
                                    .getIndexOfUserDefaultLocale(
                                        spinnerDescriptionLanguages.getContext());
                                spinnerDescriptionLanguages.setSelection(defaultLocaleIndex, true);
                            } else {
                                spinnerDescriptionLanguages.setSelection(0, true);
                            }
                        }
                    }

                } else {
                    spinnerDescriptionLanguages
                        .setSelection(description.getSelectedLanguageIndex());
                    selectedLanguages
                        .put(spinnerDescriptionLanguages, description.getLanguageCode());
                }
            }
        }
    }

    public interface Callback {

        void showAlert(int mediaDetailDescription, int descriptionInfo);
    }

    public interface EventListener {
        void onPrimaryCaptionTextChange(boolean isNotEmpty);
    }

}
