package fr.free.nrw.commons.upload;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTouch;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.ViewUtil;

import static android.view.MotionEvent.ACTION_UP;

class DescriptionsAdapter extends RecyclerView.Adapter<DescriptionsAdapter.ViewHolder> {

    List<Description> descriptions;
    List<Language> languages;
    private Context context;
    private Callback callback;

    public DescriptionsAdapter() {
        descriptions = new ArrayList<>();
        descriptions.add(new Description());
        languages = new ArrayList<>();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setDescriptions(List<Description> descriptions) {
        this.descriptions = descriptions;
        notifyDataSetChanged();
    }

    public void setLanguages(List<Language> languages) {
        this.languages = languages;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_item_description, parent, false);
        context = parent.getContext();
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.init(position);
    }

    @Override
    public int getItemCount() {
        return descriptions.size();
    }

    public List<Description> getDescriptions() {
        return descriptions;
    }

    public void addDescription(Description description) {
        this.descriptions.add(description);
        notifyItemInserted(descriptions.size() - 1);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.spinner_description_languages)
        AppCompatSpinner spinnerDescriptionLanguages;
        @BindView(R.id.et_description_text)
        EditText etDescriptionText;
        private View view;


        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.view = itemView;
        }

        public void init(int position) {
            Description description = descriptions.get(position);
            if (!TextUtils.isEmpty(description.getDescriptionText())) {
                etDescriptionText.setText(description.getDescriptionText());
            } else {
                etDescriptionText.setText("");
            }
            Drawable drawableRight = context.getResources()
                    .getDrawable(R.drawable.mapbox_info_icon_default);
            if (position != 0) {
                etDescriptionText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            } else {
                etDescriptionText.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableRight, null);
            }

            etDescriptionText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    description.setDescriptionText(editable.toString());
                }
            });

            etDescriptionText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    ViewUtil.hideKeyboard(v);
                }
            });

            SpinnerLanguagesAdapter languagesAdapter = new SpinnerLanguagesAdapter(context,
                    R.layout.row_item_languages_spinner);
            Collections.sort(languages, (language, t1) -> language.getLocale().getDisplayLanguage()
                    .compareTo(t1.getLocale().getDisplayLanguage().toString()));
            languagesAdapter.setLanguages(languages);
            languagesAdapter.notifyDataSetChanged();
            spinnerDescriptionLanguages.setAdapter(languagesAdapter);

            if (description.getSelectedLanguageIndex() == -1) {
                if (position == 0) {
                    int defaultLocaleIndex = getIndexOfUserDefaultLocale();
                    spinnerDescriptionLanguages.setSelection(defaultLocaleIndex);
                } else {
                    spinnerDescriptionLanguages.setSelection(0);
                }
            } else {
                spinnerDescriptionLanguages.setSelection(description.getSelectedLanguageIndex());
            }

            languages.get(spinnerDescriptionLanguages.getSelectedItemPosition()).setSet(true);

            //TODO do it the butterknife way
            spinnerDescriptionLanguages.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                        long l) {
                    //TODO handle case when user tries to select an already selected language
                    updateDescriptionBasedOnSelectedLanguageIndex(description, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });


        }

        @OnTouch(R.id.et_description_text)
        boolean descriptionInfo(View view, MotionEvent motionEvent) {

            if (getAdapterPosition() == 0) {
                //Description info is visible only for the first item
                final int value;
                if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                    value = etDescriptionText.getRight() - etDescriptionText
                            .getCompoundDrawables()[2]
                            .getBounds().width() - etDescriptionText.getPaddingRight();
                    if (motionEvent.getAction() == ACTION_UP && motionEvent.getX() >= value) {
                        callback.showAlert(R.string.media_detail_description,
                                R.string.description_info);
                        return true;
                    }
                } else {
                    value = etDescriptionText.getLeft() + etDescriptionText
                            .getCompoundDrawables()[0]
                            .getBounds().width();
                    if (motionEvent.getAction() == ACTION_UP && motionEvent.getRawX() <= value) {
                        callback.showAlert(R.string.media_detail_description,
                                R.string.description_info);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private int getIndexOfUserDefaultLocale() {
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).getLocale()
                    .equals(context.getResources().getConfiguration().locale)) {
                return i;
            }
        }
        return 0;
    }

    private void updateDescriptionBasedOnSelectedLanguageIndex(Description description,
            int position) {
        Language language = languages.get(position);
        Locale locale = language.getLocale();
        description.setSelectedLanguageIndex(position);
        description.setLanguageDisplayText(locale.getDisplayName());
        description.setLanguageId(locale.getLanguage());
    }

    public interface Callback {

        void showAlert(int mediaDetailDescription, int descriptionInfo);
    }
}
