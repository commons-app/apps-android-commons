package fr.free.nrw.commons.contributions;

import android.database.Cursor;

import androidx.loader.app.LoaderManager;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.Media;

/**
 * The contract for Contributions View & Presenter
 */
public class ContributionsContract {

    public interface View {

        void showWelcomeTip(boolean numberOfUploads);

        void showProgress(boolean shouldShow);

        void showNoContributionsUI(boolean shouldShow);

        void setUploadCount(int count);

        void onDataSetChanged();
    }

    public interface UserActionListener extends BasePresenter<ContributionsContract.View>,
            LoaderManager.LoaderCallbacks<Cursor> {

        Contribution getContributionsFromCursor(Cursor cursor);

        void deleteUpload(Contribution contribution);

        Media getItemAtPosition(int i);
    }
}
