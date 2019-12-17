package fr.free.nrw.commons.upload;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.BiMap;
import fr.free.nrw.commons.utils.LangCodeUtils;


public class SpinnerLanguagesAdapter extends ArrayAdapter {

    private final int resource;
    private final LayoutInflater layoutInflater;
    private List<String> languageNamesList;
    private List<String> languageCodesList;
    private final BiMap<AdapterView, String> selectedLanguages;
    public String selectedLangCode="";
    private Context context;
    private boolean dropDownClicked;
    private String savedLanguageValue;



    public SpinnerLanguagesAdapter(@NonNull Context context,
                                   int resource,
                                   BiMap<AdapterView, String> selectedLanguages,
                                   String savedLanguageValue) {
        super(context, resource);
        this.resource = resource;
        this.layoutInflater = LayoutInflater.from(context);
        languageNamesList = new ArrayList<>();
        languageCodesList = new ArrayList<>();
        prepareLanguages();
        this.selectedLanguages = selectedLanguages;
        this.context = context;
        this.dropDownClicked = false;
        this.savedLanguageValue = savedLanguageValue;
    }

    private void prepareLanguages() {
        List<Language> languages = getLocaleSupportedByDevice();

        for(Language language: languages) {
            if(!languageCodesList.contains(language.getLocale().getLanguage())) {
                languageNamesList.add(language.getLocale().getDisplayName());
                languageCodesList.add(language.getLocale().getLanguage());
            }
        }
    }

    private List<Language> getLocaleSupportedByDevice() {
        List<Language> languages = new ArrayList<>();
        Locale[] localesArray = Locale.getAvailableLocales();
        for (Locale locale : localesArray) {
            languages.add(new Language(locale));
        }

        Collections.sort(languages, (language, t1) -> language.getLocale().getDisplayName()
                .compareTo(t1.getLocale().getDisplayName()));
        return languages;
    }

    @Override
    public boolean isEnabled(int position) {
        return !languageCodesList.get(position).isEmpty()&&
                (!selectedLanguages.containsKey(languageCodesList.get(position)) ||
                        languageCodesList.get(position).equals(selectedLangCode));
    }

    @Override
    public int getCount() {
        return languageNamesList.size();
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(resource, parent, false);
        }
        ViewHolder holder = new ViewHolder(convertView);
        holder.init(position, true, savedLanguageValue);

        dropDownClicked = true;
        return convertView;
    }

    @Override
    public @NonNull
    View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(resource, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.init(position, false, savedLanguageValue);
        return convertView;
    }


    public class ViewHolder {

        @BindView(R.id.tv_language)
        TextView tvLanguage;

        @BindView(R.id.view)
        View view;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }

        public void init(int position, boolean isDropDownView, String savedLanguageValue) {
            String languageCode = LangCodeUtils.fixLanguageCode(languageCodesList.get(position));
            final String languageName = StringUtils.capitalize(languageNamesList.get(position));

            if (!isDropDownView) {
                view.setVisibility(View.GONE);
                if (languageCode.length() > 2)
                    tvLanguage.setText(languageCode.substring(0, 2));
                else
                    tvLanguage.setText(languageCode);
            } else {
                view.setVisibility(View.VISIBLE);
                if (languageCodesList.get(position).isEmpty()) {
                    tvLanguage.setText(languageName);
                    tvLanguage.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                } else {
                    tvLanguage.setText(
                            String.format("%s [%s]", languageName, languageCode));
                    if (selectedLanguages.containsKey(languageCodesList.get(position)) &&
                            !languageCodesList.get(position).equals(selectedLangCode)) {
                        tvLanguage.setTextColor(Color.GRAY);
                    }
                }
            }
        }
    }

    String getLanguageCode(int position) {
        return languageCodesList.get(position);
    }

    int getIndexOfUserDefaultLocale(Context context) {
        return languageCodesList.indexOf(context.getResources().getConfiguration().locale.getLanguage());
    }

    int getIndexOfLanguageCode(String languageCode) {
        return languageCodesList.indexOf(languageCode);
    }
}
