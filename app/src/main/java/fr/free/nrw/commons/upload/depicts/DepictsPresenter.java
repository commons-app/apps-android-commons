package fr.free.nrw.commons.upload.depicts;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;

@Singleton
public class DepictsPresenter implements DepictsContract.UserActionListener {

    private static final DepictsContract.View DUMMY = (DepictsContract.View) Proxy
            .newProxyInstance(
                    DepictsContract.View.class.getClassLoader(),
                    new Class[]{DepictsContract.View.class},
                    (proxy, method, methodArgs) -> null);

    private DepictsContract.View view = DUMMY;
    private UploadRepository repository;

    @Inject
    public DepictsPresenter(UploadRepository uploadRepository) {
        this.repository = uploadRepository;
    }

    @Override
    public void onAttachView(DepictsContract.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
    }

    @Override
    public void onNextButtonPressed() {
        view.goToNextScreen();
    }

    @Override
    public void onPreviousButtonClicked() {
        view.goToPreviousScreen();
    }

    @Override
    public void onDepictItemClicked(DepictedItem depictedItem) {
        repository.onDepictItemClicked(depictedItem);
    }

    @Override
    public void searchForDepictions(String query, List<UploadMediaDetail> mediaDetailList) {
        List<DepictedItem> depictedItemList = new ArrayList<>();
        depictedItemList.add(new DepictedItem("monument", "desc", null, false));
        depictedItemList.add(new DepictedItem("scultpture", "desc", null, false));
        depictedItemList.add(new DepictedItem("monument", "desc", null, false));
        depictedItemList.add(new DepictedItem("scultpture", "desc", null, false));
        view.setDepictsList(depictedItemList);
    }
}
