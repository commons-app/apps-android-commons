package fr.free.nrw.commons.upload;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.ui.widget.CustomEditText;
import fr.free.nrw.commons.utils.AbstractTextWatcher;
import fr.free.nrw.commons.utils.BiMap;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

public class DescriptionsAdapter extends RecyclerView.Adapter<DescriptionsAdapter.ViewHolder> {

    private List<Description> descriptions;
    private Callback callback;

    private BiMap<AdapterView, String> selectedLanguages;

    public DescriptionsAdapter() {
        descriptions = new ArrayList<>();
        selectedLanguages = new BiMap<>();
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
        CustomEditText descItemEditText;

        private View view;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.view = itemView;
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
            } else {
                descItemEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }

            descItemEditText.addTextChangedListener(new AbstractTextWatcher(
                    descriptionText -> descriptions.get(position)
                            .setDescriptionText(descriptionText)));
            descItemEditText.setDrawableClickListener(target -> {
                switch (target) {
                    case RIGHT:
                        if (getAdapterPosition() == 0) {
                            callback.showAlert(R.string.media_detail_description,
                                    R.string.description_info);
                        }
                        break;
                    default:
                        break;
                }
            });

            initLanguageSpinner(position, description);

            if (position == descriptions.size() - 1) {
                descItemEditText.requestFocus();
            }
        }

        /**
         * Extracted out the function to init the language spinner with different system supported
         * languages
         */
        private void initLanguageSpinner(int position, Description description) {
            SpinnerLanguagesAdapter languagesAdapter = new SpinnerLanguagesAdapter(
                    spinnerDescriptionLanguages.getContext(),
                    R.layout.row_item_languages_spinner, selectedLanguages);
            languagesAdapter.notifyDataSetChanged();
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
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            if (description.getSelectedLanguageIndex() == -1) {
                if (position == 0) {
                    int defaultLocaleIndex = languagesAdapter
                            .getIndexOfUserDefaultLocale(spinnerDescriptionLanguages.getContext());
                    spinnerDescriptionLanguages.setSelection(defaultLocaleIndex, true);
                } else {
                    spinnerDescriptionLanguages.setSelection(0);
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
}
