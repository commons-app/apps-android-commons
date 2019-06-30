package fr.free.nrw.commons.contributions;

import static fr.free.nrw.commons.contributions.ContributionDao.Table.ALL_FIELDS;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.contributions.ContributionsContract.UserActionListener;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * The presenter class for Contributions
 */
public class ContributionsPresenter extends DataSetObserver implements UserActionListener {

    private final ContributionsRepository repository;
    private ContributionsContract.View view;
    private Cursor cursor;

    @Inject
    ContributionsPresenter(ContributionsRepository uploadRepository) {
        this.repository = uploadRepository;
    }

    @Override
    public void onAttachView(ContributionsContract.View view) {
        this.view = view;
        if (null != cursor) {
            try {
                cursor.registerDataSetObserver(this);
            }catch (IllegalStateException e){//Cursor might be already registered
                Timber.d(e);
            }
        }
    }

    @Override
    public void onDetachView() {
        this.view = null;
        if (null != cursor) {
            try {
                cursor.unregisterDataSetObserver(this);
            }catch (Exception e){//Cursor might not be already registered
                Timber.d(e);
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(CommonsApplication.getContext(), BASE_URI,
                ALL_FIELDS, "", null,
                ContributionDao.CONTRIBUTION_SORT + "LIMIT "
                        + /*repository.get(UPLOADS_SHOWING)*/100);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        view.showProgress(false);
        if (null != cursor && cursor.getCount() > 0) {
            view.showWelcomeTip(false);
            view.showNoContributionsUI(false);
            view.setUploadCount(cursor.getCount());
        } else {
            view.showWelcomeTip(true);
            view.showNoContributionsUI(true);
        }
        swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        this.cursor = null;
        swapCursor(null);
    }

    /**
     * Get contribution from the repository
     */
    @Override
    public Contribution getContributionsFromCursor(Cursor cursor) {
        return repository.getContributionFromCursor(cursor);
    }

    /**
     * Delete a failed contribution from the local db
     * @param contribution
     */
    @Override
    public void deleteUpload(Contribution contribution) {
        repository.deleteContributionFromDB(contribution);
    }

    /**
     * Returns a contribution at the specified cursor position
     * @param i
     * @return
     */
    @Nullable
    @Override
    public Media getItemAtPosition(int i) {
        if (null != cursor && cursor.moveToPosition(i)) {
            return getContributionsFromCursor(cursor);
        }
        return null;
    }

    @Override
    public void onChanged() {
        super.onChanged();
        view.onDataSetChanged();
    }

    @Override
    public void onInvalidated() {
        super.onInvalidated();
        view.onDataSetInvalidated();
    }

    /**
     * Swap in a new Cursor, returning the old Cursor. The returned old Cursor is <em>not</em>
     * closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there was not one. If the given new
     * Cursor is the same instance is the previously set Cursor, null is also returned.
     */
    private void swapCursor(Cursor newCursor) {
        try {
            if (newCursor == cursor) {
                return;
            }
            Cursor oldCursor = cursor;
            if (oldCursor != null) {
                oldCursor.unregisterDataSetObserver(this);
            }
            cursor = newCursor;
            if (newCursor != null) {
                newCursor.registerDataSetObserver(this);
            }
            view.onDataSetChanged();
        } catch (IllegalStateException e) {//Cursor might [not] be already registered/unregistered
            Timber.e(e);
        }
    }
}
