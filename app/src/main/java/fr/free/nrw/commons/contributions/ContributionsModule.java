package fr.free.nrw.commons.contributions;

import dagger.Binds;
import dagger.Module;

/**
 * The Dagger Module for contributions related presenters and (some other objects maybe in future)
 */
@Module
public abstract class ContributionsModule {

  @Binds
  public abstract ContributionsContract.UserActionListener bindsContibutionsPresenter(
      ContributionsPresenter presenter);
}
