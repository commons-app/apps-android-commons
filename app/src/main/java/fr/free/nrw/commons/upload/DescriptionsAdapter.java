package fr.free.nrw.commons.upload;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
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

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.AbstractTextWatcher;
import fr.free.nrw.commons.utils.BiMap;
import timber.log.Timber;

public class DescriptionsAdapter extends RecyclerView.Adapter<DescriptionsAdapter.ViewHolder> {

    private List<Description> descriptions;
    private Callback callback;

    private BiMap<AdapterView, String> selectedLanguages;
    private String savedLanguageValue;

    public DescriptionsAdapter(String savedLanguageValue) {
        descriptions = new ArrayList<>();
        selectedLanguages = new BiMap<>();
        this.savedLanguageValue = savedLanguageValue;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setItems(List<Description> descriptions) {
        this.descriptions = descriptions;
        selectedLanguages = new BiMap<>();
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
        return descriptions.size();
    }

    /**
     * Gets descriptions
     *
     * @return List of descriptions
     */
    public List<Description> getDescriptions() {
        return descriptions;
    }

    public void addDescription(Description description) {
        this.descriptions.add(description);
        notifyItemInserted(descriptions.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Nullable
        @BindView(R.id.spinner_description_languages)
        AppCompatSpinner spinnerDescriptionLanguages;

        @BindView(R.id.description_item_edit_text)
        AppCompatEditText descItemEditText;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            Timber.i("descItemEditText:" + descItemEditText);
        }

        public void init(int position) {
            Description description = descriptions.get(position);
            Timber.d("Description is " + description);
            if (!TextUtils.isEmpty(description.getDescriptionText())) {
                descItemEditText.setText(description.getDescriptionText());
            } else {
                descItemEditText.setText("");
            }
            if (position == 0) {
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
                descItemEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }

            descItemEditText.addTextChangedListener(new AbstractTextWatcher(
                    descriptionText -> descriptions.get(position).setDescriptionText(descriptionText)));
            initLanguageSpinner(position, description);

            //If the description was manually added by the user, it deserves focus, if not, let the user decide
            if (description.isManuallyAdded()) {
                descItemEditText.requestFocus();
            } else {
                descItemEditText.clearFocus();
            }
        }

        /**
         * Extracted out the function to init the language spinner with different system supported languages
         * @param position
         * @param description
         */
        private void initLanguageSpinner(int position, Description description) {
            SpinnerLanguagesAdapter languagesAdapter = new SpinnerLanguagesAdapter(
                    spinnerDescriptionLanguages.getContext(),
                    R.layout.row_item_languages_spinner, selectedLanguages,
                    savedLanguageValue);
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
                            .getAdapter()).selectedLangCode = languageCode;
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
