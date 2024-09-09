package fr.free.nrw.commons.upload;

import android.content.Context;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract.View;

/**
 * The contract using which the PendingUploadsFragment or FailedUploadsFragment would communicate
 * with its PendingUploadsPresenter
 */
public class PendingUploadsContract {

    /**
     * Interface representing the view for uploads.
     */
    public interface View { }

    /**
     * Interface representing the user actions related to uploads.
     */
    public interface UserActionListener extends
        BasePresenter<fr.free.nrw.commons.upload.PendingUploadsContract.View> {

        /**
         * Deletes a upload.
         */
        void deleteUpload(Contribution contribution, Context context);
    }
}
