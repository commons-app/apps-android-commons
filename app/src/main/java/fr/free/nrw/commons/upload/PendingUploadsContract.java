package fr.free.nrw.commons.upload;

import android.content.Context;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract.View;

public class PendingUploadsContract {

    public interface View { }

    public interface UserActionListener extends
        BasePresenter<fr.free.nrw.commons.upload.PendingUploadsContract.View> {
        void deleteUpload(Contribution contribution, Context context);
    }
}
