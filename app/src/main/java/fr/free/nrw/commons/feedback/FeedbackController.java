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

    private CsrfTokenClient csrfTokenClient;
    private final PageEditClient pageEditClient;

    @Inject
    public FeedbackController(@Named(NAMED_COMMONS_CSRF) CsrfTokenClient csrfTokenClient, @Named("commons-page-edit") PageEditClient pageEditClient) {
        this.csrfTokenClient = csrfTokenClient;
        this.pageEditClient = pageEditClient;
    }

    public Observable<Boolean> postFeedback(MoreBottomSheetFragment moreBottomSheetFragment,
        Feedback feedback) {
        ApplicationlessInjection
            .getInstance(moreBottomSheetFragment.getContext())
            .getCommonsApplicationComponent()
            .inject(moreBottomSheetFragment);

        FeedbackContentCreator feedbackContentCreator = new FeedbackContentCreator(
            moreBottomSheetFragment.getContext(), feedback);

        return pageEditClient.prependEdit("Commons:Mobile_app/Feedback", feedbackContentCreator.toString(), "Summary");

//        try {
//            System.out.println("CSRF token " + token);
//            FeedbackService feedbackService = FeedbackClient.getInstance().create(FeedbackService.class);
//            feedbackService.postFeedback("/* Feedback for version "+feedback.getVersion() + " */", "addtopic", "Commons:Mobile_app/Feedback", "Feedback from " + getUserName(moreBottomSheetFragment), feedbackContentCreator.toString(), csrfTokenClient.getTokenBlocking())
//                .enqueue(new Callback<Void>() {
//                    @Override
//                    public void onResponse(Call<Void> call, Response<Void> response) {
//                        Toast.makeText(moreBottomSheetFragment.getContext(), "Your Feedback Received", Toast.LENGTH_LONG).show();
//                    }
//
//                    @Override
//                    public void onFailure(Call<Void> call, Throwable t) {
//                        Toast.makeText(moreBottomSheetFragment.getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
//                        t.printStackTrace();
//                    }
//                });
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//        }
    }
    private String getUserName(MoreBottomSheetFragment moreBottomSheetFragment){
        final AccountManager accountManager = AccountManager.get(
            moreBottomSheetFragment.getContext());
        final Account[] allAccounts = accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE);
        if (allAccounts.length != 0) {
            return allAccounts[0].name;
        }
        return "";
    }
}
