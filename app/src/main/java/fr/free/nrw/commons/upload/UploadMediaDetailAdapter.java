package fr.free.nrw.commons.upload;

import android.app.Dialog;
import android.text.Editable;
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
import fr.free.nrw.commons.upload.models.UploadMediaDetail;
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText;
import fr.free.nrw.commons.utils.AbstractTextWatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import timber.log.Timber;

public class UploadMediaDetailAdapter extends RecyclerView.Adapter<UploadMediaDetailAdapter.ViewHolder> {

    private List<UploadMediaDetail> uploadMediaDetails;
    private Callback callback;
    private EventListener eventListener;

    private HashMap<Integer, String> selectedLanguages;
    private final String savedLanguageValue;

    public UploadMediaDetailAdapter(String savedLanguageValue) {
        uploadMediaDetails = new ArrayList<>();
        selectedLanguages = new HashMap<>();
        this.savedLanguageValue = savedLanguageValue;
    }

    public UploadMediaDetailAdapter(final String savedLanguageValue,
        List<UploadMediaDetail> uploadMediaDetails) {
        this.uploadMediaDetails = uploadMediaDetails;
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

    public List<UploadMediaDetail> getItems(){
        return uploadMediaDetails;
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
        this.uploadMediaDetails.remove(uploadMediaDetail);
        notifyItemRemoved(position);
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
            captionItemEditText.setText(uploadMediaDetail.getCaptionText());
            descItemEditText.setText(uploadMediaDetail.getDescriptionText());

            if (position == 0) {
                removeButton.setVisibility(View.GONE);
                captionInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                captionInputLayout.setEndIconDrawable(R.drawable.mapbox_info_icon_default);
                captionInputLayout.setEndIconOnClickListener(v ->
                    callback.showAlert(R.string.media_detail_caption, R.string.caption_info));

                descInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                descInputLayout.setEndIconDrawable(R.drawable.mapbox_info_icon_default);
                descInputLayout.setEndIconOnClickListener(v ->
                    callback.showAlert(R.string.media_detail_description, R.string.description_info));

            } else {
                removeButton.setVisibility(View.VISIBLE);
                captionInputLayout.setEndIconDrawable(null);
                descInputLayout.setEndIconDrawable(null);
            }

            removeButton.setOnClickListener(v -> removeDescription(uploadMediaDetail, position));

            captionItemEditText.addTextChangedListener(new AbstractTextWatcher(
                    captionText -> uploadMediaDetails.get(position).setCaptionText(captionText)));
            initLanguage(position, uploadMediaDetail);

            descItemEditText.addTextChangedListener(new AbstractTextWatcher(
                    descriptionText -> uploadMediaDetails.get(position).setDescriptionText(descriptionText)));
            initLanguage(position, uploadMediaDetail);

            //If the description was manually added by the user, it deserves focus, if not, let the user decide
            if (uploadMediaDetail.isManuallyAdded()) {
                captionItemEditText.requestFocus();
            } else {
                captionItemEditText.clearFocus();
            }
        }


    private void initLanguage(int position, UploadMediaDetail description) {

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

                    listView.setAdapter(languagesAdapter);

                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                            int i2) {

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

                    listView.setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i,
                            long l) {
                            description.setSelectedLanguageIndex(i);
                            String languageCode = ((LanguagesAdapter) adapterView.getAdapter())
                                .getLanguageCode(i);
                            description.setLanguageCode(languageCode);
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
    }

    public interface Callback {

        void showAlert(int mediaDetailDescription, int descriptionInfo);
    }

    public interface EventListener {
        void onPrimaryCaptionTextChange(boolean isNotEmpty);
    }

}
