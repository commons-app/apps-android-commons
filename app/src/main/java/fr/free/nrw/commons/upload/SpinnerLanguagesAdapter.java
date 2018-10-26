package fr.free.nrw.commons.upload;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

public class SpinnerLanguagesAdapter extends ArrayAdapter {

    private final int resource;
    private final LayoutInflater layoutInflater;
    List<Language> languages;

    public SpinnerLanguagesAdapter(@NonNull Context context,
            int resource) {
        super(context, resource);
        this.resource = resource;
        this.layoutInflater = LayoutInflater.from(context);
        languages = new ArrayList<>();
    }

    public void setLanguages(List<Language> languages) {
        this.languages = languages;
    }

    @Override
    public int getCount() {
        return languages.size();
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
            Language language = languages.get(position);
            if (!isDropDownView) {
                view.setVisibility(View.GONE);
                tvLanguage.setText(
                        language.getLocale().getLanguage());
            } else {
                view.setVisibility(View.VISIBLE);
                tvLanguage.setText(
                        String.format("%s [%s]", language.getLocale().getDisplayName(),
                                language.getLocale().getLanguage()));
            }

        }
    }

}
