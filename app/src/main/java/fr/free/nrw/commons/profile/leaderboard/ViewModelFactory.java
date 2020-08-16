package fr.free.nrw.commons.profile.leaderboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import javax.inject.Inject;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private OkHttpJsonApiClient okHttpJsonApiClient;
    private SessionManager sessionManager;
    private String duration;
    private String category;
    private int limit;
    private int offset;

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String duration) {
        this.duration = duration;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    @Inject
    public ViewModelFactory(OkHttpJsonApiClient okHttpJsonApiClient, SessionManager sessionManager) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.sessionManager = sessionManager;
    }


    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LeaderboardListViewModel.class)) {
            return (T) new LeaderboardListViewModel(okHttpJsonApiClient, sessionManager,
                duration, category, limit, offset);
        }
        throw new IllegalArgumentException("Unknown class name");
    }
}
