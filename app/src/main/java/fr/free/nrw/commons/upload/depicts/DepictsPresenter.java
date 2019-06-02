package fr.free.nrw.commons.upload.depicts;

import java.lang.reflect.Proxy;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.categories.CategoriesContract;

//@Singleton
public class DepictsPresenter implements DepictsContract.UserActionListener {

    private static final DepictsContract.View DUMMY = (DepictsContract.View) Proxy
            .newProxyInstance(
                    DepictsContract.View.class.getClassLoader(),
                    new Class[]{DepictsContract.View.class},
                    (proxy, method, methodArgs) -> null);

    //private final UploadRepository repository;
    private DepictsContract.View view = DUMMY;

    @Inject
    public DepictsPresenter(UploadRepository uploadRepository){
        //this.repository = uploadRepository;
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
}
