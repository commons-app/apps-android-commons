package fr.free.nrw.commons.upload;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import java.util.ArrayList;
import java.util.List;

class DescriptionsAdapter extends RecyclerView.Adapter<DescriptionsAdapter.ViewHolder> {

    List<Description> descriptions;
    List<Language> languages;

    public DescriptionsAdapter() {
        descriptions = new ArrayList<>();
        languages = new ArrayList<>();
    }

    public void setDescriptions(List<Description> descriptions) {
        this.descriptions = descriptions;
    }

    public void setLanguages(List<Language> languages) {
        this.languages = languages;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_item_description, parent, false);
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


    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.spinner_description_languages)
        AppCompatSpinner spinnerDescriptionLanguages;
        @BindView(R.id.et_description_text)
        EditText etDescriptionText;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void init(int position) {
            Description description = descriptions.get(position);
            if (!TextUtils.isEmpty(description.getDescriptionText())) {
                etDescriptionText.setText(description.getDescriptionText());
            } else {
                etDescriptionText.setText("");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                    R.layout.row_item_languages_spinner) {

                @Override
                public View getDropDownView(int position, View convertView,
                        ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView tvLanguage = view.findViewById(R.id.et_language);
                    tvLanguage.setText(languages.get(position).getDisplayText());
                    return view;
                }
            };

            spinnerDescriptionLanguages.setAdapter(adapter);

            spinnerDescriptionLanguages.setSelection(description.getSelectedLanguageIndex());

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
    }

    private void updateDescriptionBasedOnSelectedLanguageIndex(Description description,
            int position) {
        Language language = languages.get(position);
        description.setSelectedLanguageIndex(position);
        description.setLanguageDisplayText(language.getDisplayText());
        description.setLanguageId(language.getId());
    }
}
