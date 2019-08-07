package fr.free.nrw.commons.depictions;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class DepictionModule {
    @Binds
    public abstract DepictedImagesContract.UserActionListener bindsDepictedImagesPresenter(
            DepictedImagesPresenter
                    presenter
    );
}
