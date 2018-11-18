package fr.free.nrw.commons.upload;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.BiMap;

public class SpinnerLanguagesAdapter extends ArrayAdapter {

    private final int resource;
    private final LayoutInflater layoutInflater;
    private List<String> languageNamesList;
    private List<String> languageCodesList;
    private final BiMap<AdapterView, String> selectedLanguages;
    public String selectedLangCode="";



    public SpinnerLanguagesAdapter(@NonNull Context context,
                                   int resource, BiMap<AdapterView, String> selectedLanguages) {
        super(context, resource);
        this.resource = resource;
        this.layoutInflater = LayoutInflater.from(context);
        languageNamesList = new ArrayList<>();
        languageCodesList = new ArrayList<>();
        prepareLanguages(context);
        this.selectedLanguages = selectedLanguages;
    }

    private void prepareLanguages(Context context) {
        Resources resources = context.getResources();
        List<Language> languages = getLocaleSupportedByDevice();

        for(Language language: languages) {
            languageNamesList.add(language.getLocale().getDisplayName());
            languageCodesList.add(language.getLocale().getDisplayScript());
        }
        for (int i = 0; i < languages.size(); i++) {
            languageNamesList.add(resources.getString(Language.languageGroups[i]));
            languageCodesList.add("");
            languageNamesList.addAll(Arrays.asList(resources.getStringArray(Language.languageNames[i])));
            languageCodesList.addAll(Arrays.asList(resources.getStringArray(Language.languageCodes[i])));
        }
    }

    private List<Language> getLocaleSupportedByDevice() {
        List<Language> languages = new ArrayList<>();
        Locale[] localesArray = Locale.getAvailableLocales();
        for (Locale locale : localesArray) {
            languages.add(new Language(locale));
        }
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
        View view = layoutInflater.inflate(resource, parent, false);
        ViewHolder holder = new ViewHolder(view);
        holder.init(position, true);
        return view;
    }

    @Override
    public @NonNull
    View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = layoutInflater.inflate(resource, parent, false);
        ViewHolder holder = new ViewHolder(view);
        holder.init(position, false);
        return view;
    }


    public class ViewHolder {

        @BindView(R.id.ll_container_description_language)
        LinearLayout llContainerDescriptionLanguage;

        @BindView(R.id.tv_language)
        TextView tvLanguage;

        @BindView(R.id.view)
        View view;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }

        public void init(int position, boolean isDropDownView) {
            if (!isDropDownView) {
                view.setVisibility(View.GONE);
                if(languageCodesList.get(position).length()>2)
                    tvLanguage.setText(languageCodesList.get(position).subSequence(0,2));
                else
                    tvLanguage.setText(languageCodesList.get(position));

            } else {
                view.setVisibility(View.VISIBLE);
                if (languageCodesList.get(position).isEmpty()) {
                    tvLanguage.setText(languageNamesList.get(position));
                    tvLanguage.setTextColor(Color.GRAY);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        tvLanguage.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    }
                } else {
                    tvLanguage.setText(
                            String.format("%s [%s]", languageNamesList.get(position), languageCodesList.get(position)));
                    if(selectedLanguages.containsKey(languageCodesList.get(position))&&
                            !languageCodesList.get(position).equals(selectedLangCode))
                        tvLanguage.setTextColor(Color.GRAY);
                    else
                        tvLanguage.setTextColor(Color.BLACK);
                }
            }
        }
    }

    public String getLanguageCode(int position) {
        return languageCodesList.get(position);
    }

    public int getIndexOfUserDefaultLocale(Context context) {
        return languageCodesList.indexOf(context.getResources().getConfiguration().locale.getLanguage());
    }

}
