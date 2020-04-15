package org.wikipedia.dataclient.restbase.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.page.Section;

import java.util.Collections;
import java.util.List;

/**
 * Gson POJO for loading remaining page content.
 */
public class RbPageRemaining implements PageRemaining {
    @SuppressWarnings("unused") @Nullable private List<Section> sections;

    @NonNull @Override public List<Section> sections() {
        if (sections == null) {
            return Collections.emptyList();
        }
        return sections;
    }
}
