package fr.free.nrw.commons.upload;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import androidx.annotation.NonNull;
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

public class CaptionsAdapter extends RecyclerView.Adapter<CaptionsAdapter.ViewHolder> {

    private List<Caption> captions;
    private Callback callback;

    private BiMap<AdapterView, String> selectedLanguages;

    public CaptionsAdapter() {
        this.captions = new ArrayList<>();
        this.selectedLanguages = new BiMap<>();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setItems(List<Caption> captions) {
        this.captions = captions;
        selectedLanguages = new BiMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_item_caption, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.init(position);
    }

    @Override
    public int getItemCount() {
        return captions.size();
    }

    public List<Caption> getCaptions() {
        return captions;
    }

    public interface Callback {

        void showAlert(int mediaDetailDescription, int descriptionInfo);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.spinner_caption_languages)
        AppCompatSpinner spinnerCaptionLanguge;

        @BindView(R.id.caption_item_edit_text)
        AppCompatEditText captionItemEditText;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void init(int position) {
            Caption caption = captions.get(position);
            Timber.d("Description is " + caption);
            if (!TextUtils.isEmpty(caption.getCaptionText())) {
                captionItemEditText.setText(caption.getCaptionText());
            } else {
                captionItemEditText.setText("");
            }
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

            } else {
                captionItemEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }

            captionItemEditText.addTextChangedListener(new AbstractTextWatcher(
                    descriptionText -> captions.get(position)
                            .setCaptionText(descriptionText)));
            initLanguageSpinner(position, caption);

            //If the description was manually added by the user, it deserves focus, if not, let the user decide
            if (caption.isManuallyAdded()) {
                captionItemEditText.requestFocus();
            } else {
                captionItemEditText.clearFocus();
            }
        }

        private void initLanguageSpinner(int position, Caption caption) {
            SpinnerLanguagesAdapter languagesAdapter = new SpinnerLanguagesAdapter(
                    spinnerCaptionLanguge.getContext(),
                    R.layout.row_item_languages_spinner, selectedLanguages);
            languagesAdapter.notifyDataSetChanged();
            spinnerCaptionLanguge.setAdapter(languagesAdapter);

            spinnerCaptionLanguge.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                                           long l) {
                    caption.setSelectedLanguageIndex(position);
                    String languageCode = ((SpinnerLanguagesAdapter) adapterView.getAdapter())
                            .getLanguageCode(position);
                    caption.setLanguageCode(languageCode);
                    selectedLanguages.remove(adapterView);
                    selectedLanguages.put(adapterView, languageCode);
                    ((SpinnerLanguagesAdapter) adapterView
                            .getAdapter()).selectedLangCode = languageCode;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            if (caption.getSelectedLanguageIndex() == -1) {
                if (position == 0) {
                    int defaultLocaleIndex = languagesAdapter
                            .getIndexOfUserDefaultLocale(spinnerCaptionLanguge.getContext());
                    spinnerCaptionLanguge.setSelection(defaultLocaleIndex, true);
                } else {
                    spinnerCaptionLanguge.setSelection(0);
                }
            } else {
                spinnerCaptionLanguge.setSelection(caption.getSelectedLanguageIndex());
                selectedLanguages.put(spinnerCaptionLanguge, caption.getLanguageCode());
            }
        }

        private Drawable getInfoIcon() {
            return captionItemEditText.getContext()
                    .getResources()
                    .getDrawable(R.drawable.mapbox_info_icon_default);
        }

    }
}
