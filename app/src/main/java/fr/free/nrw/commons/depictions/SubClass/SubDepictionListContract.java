package fr.free.nrw.commons.depictions.SubClass;

import java.io.IOException;
import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;

public interface SubDepictionListContract {

    interface View {

        void onImageUrlFetched(String response, int position);

        void onSuccess(List<DepictedItem> mediaList);

        void initErrorView();

        void showSnackbar();

        void setIsLastPage(boolean b);

        boolean isParentDepiction();
    }

    interface UserActionListener extends BasePresenter<View> {

        void saveQuery();

        void fetchThumbnailForEntityId(String entityId, int position);

        void initSubDepictionList() throws IOException;

        String getQuery();
    }
}
