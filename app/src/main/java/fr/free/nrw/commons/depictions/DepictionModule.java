package fr.free.nrw.commons.depictions;

import dagger.Binds;
import dagger.Module;
import fr.free.nrw.commons.depictions.Media.DepictedImagesContract;
import fr.free.nrw.commons.depictions.Media.DepictedImagesPresenter;
import fr.free.nrw.commons.depictions.subClass.SubDepictionListContract;
import fr.free.nrw.commons.depictions.subClass.SubDepictionListPresenter;

/**
 * The Dagger Module for explore:depictions related presenters and (some other objects maybe in
 * future)
 */
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
