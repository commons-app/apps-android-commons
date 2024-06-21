package fr.free.nrw.commons.upload;

import android.content.Context;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.contributions.Contribution;

public class PendingUploadsContract {

    public interface View { }

    public interface UserActionListener extends
        BasePresenter<fr.free.nrw.commons.upload.PendingUploadsContract.View> {

        void deleteUpload(Contribution contribution, Context context);
    }
}
