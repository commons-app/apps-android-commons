package fr.free.nrw.commons.feedback;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment;
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText;
import java.util.Date;
import javax.inject.Inject;
import org.wikipedia.csrf.CsrfTokenClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedbackDialog extends Dialog {

    @BindView(R.id.btn_submit_feedback)
    Button button;
    @BindView(R.id.api_level_checkbox)
    CheckBox apiLevel;
    @BindView(R.id.android_version_checkbox)
    CheckBox androidVersion;
    @BindView(R.id.device_manufacturer_checkbox)
    CheckBox deviceManufacturer;
    @BindView(R.id.device_model_checkbox)
    CheckBox deviceModel;
    @BindView(R.id.device_name_checkbox)
    CheckBox deviceName;
    @BindView(R.id.network_type_checkbox)
    CheckBox networkType;
    @BindView(R.id.app_version_name_checkbox)
    CheckBox appVersionName;
    @BindView(R.id.username_checkbox)
    CheckBox userName;
    @BindView(R.id.feedback_item_edit_text)
    PasteSensitiveTextInputEditText feedbackDescription;

    @Inject
    FeedbackController feedbackController;

    private MoreBottomSheetFragment moreBottomSheetFragment;
    private OnFeedbackSubmitCallback onFeedbackSubmitCallback;

    public FeedbackDialog(Context context, OnFeedbackSubmitCallback onFeedbackSubmitCallback) {
        super(context);
        this.moreBottomSheetFragment = moreBottomSheetFragment;
        this.onFeedbackSubmitCallback = onFeedbackSubmitCallback;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_feedback);
        ButterKnife.bind(this);

    }

    @OnClick(R.id.btn_submit_feedback)
    void submitFeedback() {
        System.out.println("INSIDE");
        onFeedbackSubmitCallback.onFeedbackSubmit(new Feedback("1.1", "28", "testing", "28", "redmit", "redmi", "device", "wigi", new Date().toString()));
    }

}
