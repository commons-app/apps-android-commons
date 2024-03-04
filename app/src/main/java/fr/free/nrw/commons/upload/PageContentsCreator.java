package fr.free.nrw.commons.upload;

import android.content.Context;
import androidx.annotation.NonNull;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile.DateTimeWithSource;
import fr.free.nrw.commons.settings.Prefs.Licenses;
import fr.free.nrw.commons.utils.ConfigUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import timber.log.Timber;

public class PageContentsCreator {

    //{{According to Exif data|2009-01-09}}
    private static final String TEMPLATE_DATE_ACC_TO_EXIF = "{{According to Exif data|%s}}";

    //2009-01-09 → 9 January 2009
    private static final String TEMPLATE_DATA_OTHER_SOURCE = "%s";

    private final Context context;

    @Inject
    public PageContentsCreator(final Context context) {
        this.context = context;
    }

    public String createFrom(final Contribution contribution) {
        StringBuilder buffer = new StringBuilder();
        final Media media = contribution.getMedia();
        buffer
            .append("== {{int:filedesc}} ==\n")
            .append("{{Information\n")
            .append("|description=").append(media.getFallbackDescription()).append("\n");
        if (contribution.getWikidataPlace() != null) {
            buffer.append("{{ on Wikidata|").append(contribution.getWikidataPlace().getId())
                .append("}}");
        }
        buffer
            .append("|source=").append("{{own}}\n")
            .append("|author=[[User:").append(media.getAuthor()).append("|")
            .append(media.getAuthor()).append("]]\n");

        final String templatizedCreatedDate = getTemplatizedCreatedDate(
            contribution.getDateCreatedString(), contribution.getDateCreated(), contribution.getDateCreatedSource());
        if (!StringUtils.isBlank(templatizedCreatedDate)) {
            buffer.append("|date=").append(templatizedCreatedDate);
        }

        buffer.append("}}").append("\n");

        //Only add Location template (e.g. {{Location|37.51136|-77.602615}} ) if coords is not null
        final String decimalCoords = contribution.getDecimalCoords();
        if (decimalCoords != null) {
            buffer.append("{{Location|").append(decimalCoords).append("}}").append("\n");
        }

        if (contribution.getWikidataPlace() != null && contribution.getWikidataPlace().isMonumentUpload()) {
            buffer.append(String.format(Locale.ENGLISH, "{{Wiki Loves Monuments %d|1= %s}}\n",
                Utils.getWikiLovesMonumentsYear(Calendar.getInstance()), contribution.getCountryCode()));
        }

        buffer
            .append("\n")
            .append("== {{int:license-header}} ==\n")
            .append(licenseTemplateFor(media.getLicense())).append("\n\n")
            .append("{{Uploaded from Mobile|platform=Android|version=")
            .append(ConfigUtils.getVersionNameWithSha(context)).append("}}\n");
        final List<String> categories = media.getCategories();
        if (categories != null && categories.size() != 0) {
            for (int i = 0; i < categories.size(); i++) {
                buffer.append("\n[[Category:").append(categories.get(i)).append("]]");
            }
        } else {
            buffer.append("{{subst:unc}}");
        }
        Timber.d("Template: %s", buffer.toString());
        return buffer.toString();
    }

    /**
     * Returns upload date in either TEMPLATE_DATE_ACC_TO_EXIF or TEMPLATE_DATA_OTHER_SOURCE
     *
     * @param dateCreated
     * @param dateCreatedSource
     * @return
     */
    private String getTemplatizedCreatedDate(String dateCreatedString, Date dateCreated, String dateCreatedSource) {
        if (dateCreated != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return String.format(Locale.ENGLISH,
                isExif(dateCreatedSource) ? TEMPLATE_DATE_ACC_TO_EXIF : TEMPLATE_DATA_OTHER_SOURCE,
                isExif(dateCreatedSource) ? dateCreatedString: dateFormat.format(dateCreated)
            ) + "\n";
        }
        return "";
    }

    private boolean isExif(String dateCreatedSource) {
        return DateTimeWithSource.EXIF_SOURCE.equals(dateCreatedSource);
    }

    @NonNull
    private String licenseTemplateFor(String license) {
        switch (license) {
            case Licenses.CC_BY_3:
                return "{{self|cc-by-3.0}}";
            case Licenses.CC_BY_4:
                return "{{self|cc-by-4.0}}";
            case Licenses.CC_BY_SA_3:
                return "{{self|cc-by-sa-3.0}}";
            case Licenses.CC_BY_SA_4:
                return "{{self|cc-by-sa-4.0}}";
            case Licenses.CC0:
                return "{{self|cc-zero}}";
        }

        throw new RuntimeException("Unrecognized license value: " + license);
    }
}
