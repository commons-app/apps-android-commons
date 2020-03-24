package fr.free.nrw.commons.upload;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
        holder.init(position);
    }

    @Override
    public int getItemCount() {
        return uploadMediaDetails.size();
    }

    /**
     * Gets descriptions
     *
     * @return List of descriptions
     */
    public List<UploadMediaDetail> getUploadMediaDetails() {
        return uploadMediaDetails;
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

        @BindView(R.id.caption_item_edit_text)
        AppCompatEditText captionItemEditText;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            Timber.i("descItemEditText:" + descItemEditText);
        }

        public void init(int position) {
            UploadMediaDetail uploadMediaDetail = uploadMediaDetails.get(position);
            Timber.d("UploadMediaDetail is " + uploadMediaDetail);
            if (!TextUtils.isEmpty(uploadMediaDetail.getCaptionText())) {
                captionItemEditText.setText(uploadMediaDetail.getCaptionText());
            } else {
                captionItemEditText.setText("");
            }

            if (!TextUtils.isEmpty(uploadMediaDetail.getDescriptionText())) {
                descItemEditText.setText(uploadMediaDetail.getDescriptionText());
            } else {
                descItemEditText.setText("");
            }

            captionItemEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() != 0) {
                        eventListener.onEvent(true);
                    } else eventListener.onEvent(false);
                }
            });

            if (position == 0) {
                captionItemEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, getInfoIcon(),
                        null);
                captionItemEditText.setOnTouchListener((v, event) -> {
                    //2 is for drawable right
                    if (event.getAction() == MotionEvent.ACTION_UP && (event.getRawX() >= (captionItemEditText.getRight() - captionItemEditText.getCompoundDrawables()[2].getBounds().width()))) {
                        if (getAdapterPosition() == 0) {
                            callback.showAlert(R.string.media_detail_caption,
                                    R.string.caption_info);
                        }
                        return true;
                    }
                    return false;
                });

                descItemEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, getInfoIcon(),
                        null);
                descItemEditText.setOnTouchListener((v, event) -> {
                    //2 is for drawable right
                    float twelveDpInPixels = convertDpToPixel(12, descItemEditText.getContext());
                    if (event.getAction() == MotionEvent.ACTION_UP && descItemEditText.getCompoundDrawables()[2].getBounds().contains((int)(descItemEditText.getWidth()-(event.getX()+twelveDpInPixels)),(int)(event.getY()-twelveDpInPixels))){
                        if (getAdapterPosition() == 0) {
                            callback.showAlert(R.string.media_detail_description,
                                    R.string.description_info);
                        }
                        return true;
                    }
                    return false;
                });

            } else {
                captionItemEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                descItemEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }

            captionItemEditText.addTextChangedListener(new AbstractTextWatcher(
                    captionText -> uploadMediaDetails.get(position)
                            .setCaptionText(captionText)));
            initLanguageSpinner(position, uploadMediaDetail);

            descItemEditText.addTextChangedListener(new AbstractTextWatcher(
                    descriptionText -> uploadMediaDetails.get(position)
                            .setDescriptionText(descriptionText)));
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

            if (description.getSelectedLanguageIndex() == -1) {
                if (!TextUtils.isEmpty(savedLanguageValue)) {
                    // If user has chosen a default language from settings activity savedLanguageValue is not null
                    spinnerDescriptionLanguages.setSelection(languagesAdapter.getIndexOfLanguageCode(savedLanguageValue));
                } else {
                    if (position == 0) {
                        int defaultLocaleIndex = languagesAdapter
                                .getIndexOfUserDefaultLocale(spinnerDescriptionLanguages.getContext());
                        spinnerDescriptionLanguages.setSelection(defaultLocaleIndex, true);
                    } else {
                        spinnerDescriptionLanguages.setSelection(0,true);
                    }
                }

            } else {
                spinnerDescriptionLanguages.setSelection(description.getSelectedLanguageIndex());
                selectedLanguages.put(spinnerDescriptionLanguages, description.getLanguageCode());
            }
        }

        /**
         * Extracted out the method to get the icon drawable
         */
        private Drawable getInfoIcon() {
            return descItemEditText.getContext()
                    .getResources()
                    .getDrawable(R.drawable.mapbox_info_icon_default);
        }
    }

    public interface Callback {

        void showAlert(int mediaDetailDescription, int descriptionInfo);
    }

    public interface EventListener {
        void onEvent(Boolean data);
    }

    /**
     * converts dp to pixel
     * @param dp
     * @param context
     * @return
     */
    private float convertDpToPixel(float dp, Context context) {
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
