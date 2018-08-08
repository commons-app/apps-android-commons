package fr.free.nrw.commons.upload;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTouch;
import butterknife.Optional;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.AbstractTextWatcher;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

import static android.view.MotionEvent.ACTION_UP;

class DescriptionsAdapter extends RecyclerView.Adapter<DescriptionsAdapter.ViewHolder> {

    Title title;
    List<Description> descriptions;
    private Context context;
    private Callback callback;

    public DescriptionsAdapter() {
        title = new Title();
        descriptions = new ArrayList<>();
        descriptions.add(new Description());
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setItems(Title title, List<Description> descriptions) {
        this.descriptions = descriptions;
        this.title = title;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return 1;
        else return 2;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_item_title, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_item_description, parent, false);
        }
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.init(position);
    }

    @Override
    public int getItemCount() {
        return descriptions.size() + 1;
    }

    public List<Description> getDescriptions() {
        return descriptions;
    }

    public void addDescription(Description description) {
        this.descriptions.add(description);
        notifyItemInserted(descriptions.size() + 1);
    }

    public Title getTitle() {
        return title;
    }

    public void setTitle(Title title) {
        this.title = title;
        notifyItemInserted(0);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Nullable
        @BindView(R.id.spinner_description_languages)
        AppCompatSpinner spinnerDescriptionLanguages;

        @BindView(R.id.description_item_edit_text)
        EditText descItemEditText;

        private View view;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.view = itemView;
            Timber.i("descItemEditText:" + descItemEditText + " and descItemEditText:" + descItemEditText);
        }

        public void init(int position) {
            if (position == 0) {
                Timber.d("Title is " + title);
                if (!title.isEmpty()) {
                    descItemEditText.setText(title.toString());
                } else {
                    descItemEditText.setText("");
                }
                descItemEditText.addTextChangedListener(new AbstractTextWatcher(title::setTitleText));

                descItemEditText.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus) {
                        ViewUtil.hideKeyboard(v);
                    }
                });

            } else {
                Description description = descriptions.get(position - 1);
                Timber.d("Description is " + description);
                if (!TextUtils.isEmpty(description.getDescriptionText())) {
                    descItemEditText.setText(description.getDescriptionText());
                } else {
                    descItemEditText.setText("");
                }
                Drawable drawableRight = context.getResources()
                        .getDrawable(R.drawable.mapbox_info_icon_default);
                if (position == 1) {
                    descItemEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableRight, null);
                } else {
                    descItemEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }

                descItemEditText.addTextChangedListener(new AbstractTextWatcher(description::setDescriptionText));

                descItemEditText.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus) {
                        ViewUtil.hideKeyboard(v);
                    }
                });

                SpinnerLanguagesAdapter languagesAdapter = new SpinnerLanguagesAdapter(context,
                        R.layout.row_item_languages_spinner);
                languagesAdapter.notifyDataSetChanged();
                spinnerDescriptionLanguages.setAdapter(languagesAdapter);

                if (description.getSelectedLanguageIndex() == -1) {
                    if (position == 1) {
                        int defaultLocaleIndex = languagesAdapter.getIndexOfUserDefaultLocale(context);
                        spinnerDescriptionLanguages.setSelection(defaultLocaleIndex);
                    } else {
                        spinnerDescriptionLanguages.setSelection(0);
                    }
                } else {
                    spinnerDescriptionLanguages.setSelection(description.getSelectedLanguageIndex());
                }

                //TODO do it the butterknife way
                spinnerDescriptionLanguages.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                                               long l) {
                        //TODO handle case when user tries to select an already selected language
                        description.setSelectedLanguageIndex(position);
                        String languageCode = ((SpinnerLanguagesAdapter) adapterView.getAdapter()).getLanguageCode(position);
                        description.setLanguageCode(languageCode);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }

        }

        @Optional
        @OnTouch(R.id.description_item_edit_text)
        boolean descriptionInfo(View view, MotionEvent motionEvent) {
            if (getAdapterPosition() == 1) {
                //Description info is visible only for the first item
                final int value;
                if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                    value = view.getRight() - descItemEditText
                            .getCompoundDrawables()[2]
                            .getBounds().width() - etDescriptionText.getPaddingRight();
                    if (motionEvent.getAction() == ACTION_UP && motionEvent.getX() >= value) {
                        callback.showAlert(R.string.media_detail_description,
                                R.string.description_info);
                        return true;
                    }
                } else {
                    value = descItemEditText.getLeft() + descItemEditText
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

    public interface Callback {
        void showAlert(int mediaDetailDescription, int descriptionInfo);
    }
}
