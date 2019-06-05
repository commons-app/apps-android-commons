package fr.free.nrw.commons.upload.structure.depicts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GpsDepictsModel {

    private Set<String> depictsSet;

    @Inject
    public GpsDepictsModel() {
        clear();
    }

    public void clear() {
        depictsSet = new HashSet<>();
    }

    public boolean getGpsDepictExists() {
        return !depictsSet.isEmpty();
    }

    public List<String> getCategoryList() {
        return new ArrayList<>(depictsSet);
    }

    public void setCategoryList(List<String> categoryList) {
        clear();
        depictsSet.addAll(categoryList != null ? categoryList : new ArrayList<>());
    }

    public void add(String categoryString) {
        depictsSet.add(categoryString);
    }

}
