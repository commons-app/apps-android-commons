package fr.free.nrw.commons.upload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GpsCategoryModel {
    private Set<String> categorySet;

    @Inject
    public GpsCategoryModel() {
        clear();
    }

    public void clear() {
        categorySet = new HashSet<>();
    }

    public boolean getGpsCatExists() {
        return !categorySet.isEmpty();
    }

    public List<String> getCategoryList() {
        return new ArrayList<>(categorySet);
    }

    public void setCategoryList(List<String> categoryList) {
        clear();
        categorySet.addAll(categoryList != null ? categoryList : new ArrayList<>());
    }
}
