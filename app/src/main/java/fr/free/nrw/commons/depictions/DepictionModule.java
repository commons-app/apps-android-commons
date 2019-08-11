package fr.free.nrw.commons.depictions;

import dagger.Binds;
import dagger.Module;
import fr.free.nrw.commons.depictions.Media.DepictedImagesContract;
import fr.free.nrw.commons.depictions.Media.DepictedImagesPresenter;
import fr.free.nrw.commons.depictions.SubClass.SubDepictionListContract;
import fr.free.nrw.commons.depictions.SubClass.SubDepictionListPresenter;

@Module
public abstract class DepictionModule {

    @Binds
    public abstract DepictedImagesContract.UserActionListener bindsDepictedImagesPresenter(
            DepictedImagesPresenter
                    presenter
    );

    @Binds
    public abstract SubDepictionListContract.UserActionListener bindsSubDepictionListPresenter(
            SubDepictionListPresenter
            presenter
    );
}
