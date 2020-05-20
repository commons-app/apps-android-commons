package fr.free.nrw.commons.depictions.subClass;

import java.io.IOException;
import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;

/**
 * The contract with which SubDepictionListFragment and its presenter would talk to each other
 */
public interface SubDepictionListContract {

    interface View {

        void onSuccess(List<DepictedItem> mediaList);

        void initErrorView();

        void showSnackbar();

        void setIsLastPage(boolean b);

    }

    interface UserActionListener extends BasePresenter<View> {

        void initSubDepictionList(String qid, Boolean isParentClass) throws IOException;

        String getQuery();
    }
}
