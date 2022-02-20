package fr.free.nrw.commons.feedback;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.widget.Toast;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.wikipedia.csrf.CsrfTokenClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class FeedbackController {

    private CsrfTokenClient csrfTokenClient;

    @Inject
    public FeedbackController(@Named(NAMED_COMMONS_CSRF) CsrfTokenClient csrfTokenClient) {
        this.csrfTokenClient = csrfTokenClient;
    }

    public void postFeedback(MoreBottomSheetFragment moreBottomSheetFragment,
        Feedback feedback) {
        ApplicationlessInjection
            .getInstance(moreBottomSheetFragment.getContext())
            .getCommonsApplicationComponent()
            .inject(moreBottomSheetFragment);

        String token = null;
        FeedbackContentCreator feedbackContentCreator = new FeedbackContentCreator(
            moreBottomSheetFragment.getContext(), feedback);

        try {
            token = csrfTokenClient.getTokenBlocking();
            System.out.println("CSRF token " + token);
            FeedbackService feedbackService = FeedbackClient.getInstance().create(FeedbackService.class);
            feedbackService.postFeedback("/* Feedback for version "+feedback.getVersion() + " */", "addtopic", "Commons:Mobile_app/Feedback", "Feedback from " + getUserName(moreBottomSheetFragment), feedbackContentCreator.toString(), csrfTokenClient.getTokenBlocking())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        try {
                            System.out.println(response.raw().body().string() + " ");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(moreBottomSheetFragment.getContext(), "Your Feedback Received", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(moreBottomSheetFragment.getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
                        t.printStackTrace();
                    }
                });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
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
