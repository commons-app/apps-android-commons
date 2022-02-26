package fr.free.nrw.commons.feedback;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

import android.accounts.Account;
import android.accounts.AccountManager;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.actions.PageEditClient;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment;
import io.reactivex.Observable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.wikipedia.csrf.CsrfTokenClient;

@Singleton
public class FeedbackController {

    private final PageEditClient pageEditClient;

    @Inject
    public FeedbackController(@Named("commons-page-edit") PageEditClient pageEditClient) {
        this.pageEditClient = pageEditClient;
    }

    public Observable<Boolean> postFeedback(MoreBottomSheetFragment moreBottomSheetFragment,
        Feedback feedback) {
        ApplicationlessInjection
            .getInstance(moreBottomSheetFragment.getContext())
            .getCommonsApplicationComponent()
            .inject(moreBottomSheetFragment);

        FeedbackContentCreator feedbackContentCreator = new FeedbackContentCreator(feedback);

        return pageEditClient.prependEdit("Commons:Mobile_app/Feedback", feedbackContentCreator.toString(), "Summary");
    }
}
