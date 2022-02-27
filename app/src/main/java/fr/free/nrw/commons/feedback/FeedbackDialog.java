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
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.DeviceInfoUtil;
import java.util.Date;
import javax.inject.Inject;
import org.wikipedia.csrf.CsrfTokenClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Feedback dialog that asks user for message and
 * other device specifications
 */
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
    @BindView(R.id.username_checkbox)
    CheckBox userName;
    @BindView(R.id.feedback_item_edit_text)
    PasteSensitiveTextInputEditText feedbackDescription;

    @Inject
    FeedbackController feedbackController;

    private OnFeedbackSubmitCallback onFeedbackSubmitCallback;

    public FeedbackDialog(Context context, OnFeedbackSubmitCallback onFeedbackSubmitCallback) {
        super(context);
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
        if(feedbackDescription.getText().toString().equals("")) {
            feedbackDescription.setError(getContext().getString(R.string.enter_description));
            return;
        }
        String mAppVersionVersion = ConfigUtils.getVersionNameWithSha(getContext());
        String mAndroidVersion = androidVersion.isChecked() ? DeviceInfoUtil.getAndroidVersion() : null;
        String mAPILevel = apiLevel.isChecked() ? DeviceInfoUtil.getAPILevel() : null;
        String mDeviceManufacturer = deviceManufacturer.isChecked() ? DeviceInfoUtil.getDeviceManufacturer() : null;
        String mDeviceModel = deviceModel.isChecked() ? DeviceInfoUtil.getDeviceModel() : null;
        String mDeviceName = deviceName.isChecked() ? DeviceInfoUtil.getDevice() : null;
        String mNetworkType = networkType.isChecked() ? DeviceInfoUtil.getConnectionType(getContext()).toString() : null;
        Feedback feedback = new Feedback(mAppVersionVersion,mAPILevel, feedbackDescription.getText().toString(), mAndroidVersion, mDeviceModel, mDeviceManufacturer, mDeviceName, mNetworkType);;
        onFeedbackSubmitCallback.onFeedbackSubmit(feedback);
        dismiss();
    }

}
