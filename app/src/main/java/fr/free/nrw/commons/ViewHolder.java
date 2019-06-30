package fr.free.nrw.commons;

import fr.free.nrw.commons.contributions.model.DisplayableContribution;

public interface ViewHolder<T> {
    void init(int position,
            DisplayableContribution contribution);
}
