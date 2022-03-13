package fr.free.nrw.commons.feedback;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.databinding.DialogFeedbackBinding;
import fr.free.nrw.commons.feedback.model.Feedback;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.DeviceInfoUtil;

/**
 * Feedback dialog that asks user for message and
 * other device specifications
 */
public class FeedbackDialog extends Dialog {
    DialogFeedbackBinding dialogFeedbackBinding;

    private OnFeedbackSubmitCallback onFeedbackSubmitCallback;

    public FeedbackDialog(Context context, OnFeedbackSubmitCallback onFeedbackSubmitCallback) {
        super(context);
        this.onFeedbackSubmitCallback = onFeedbackSubmitCallback;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialogFeedbackBinding = DialogFeedbackBinding.inflate(getLayoutInflater());
        final View view = dialogFeedbackBinding.getRoot();
        setContentView(view);
    }

    /**
     * When the button is clicked, it will create a feedback object
     * and give a callback to calling activity/fragment
     */
    @OnClick(R.id.btn_submit_feedback)
    void submitFeedback() {
        if(dialogFeedbackBinding.feedbackItemEditText.getText().toString().equals("")) {
            dialogFeedbackBinding.feedbackItemEditText.setError(getContext().getString(R.string.enter_description));
            return;
        }
        String mAppVersionVersion = ConfigUtils.getVersionNameWithSha(getContext());
        String mAndroidVersion = dialogFeedbackBinding.androidVersionCheckbox.isChecked() ? DeviceInfoUtil.getAndroidVersion() : null;
        String mAPILevel = dialogFeedbackBinding.apiLevelCheckbox.isChecked() ? DeviceInfoUtil.getAPILevel() : null;
        String mDeviceManufacturer = dialogFeedbackBinding.deviceManufacturerCheckbox.isChecked() ? DeviceInfoUtil.getDeviceManufacturer() : null;
        String mDeviceModel = dialogFeedbackBinding.deviceModelCheckbox.isChecked() ? DeviceInfoUtil.getDeviceModel() : null;
        String mDeviceName = dialogFeedbackBinding.deviceNameCheckbox.isChecked() ? DeviceInfoUtil.getDevice() : null;
        String mNetworkType = dialogFeedbackBinding.networkTypeCheckbox.isChecked() ? DeviceInfoUtil.getConnectionType(getContext()).toString() : null;
        Feedback feedback = new Feedback(mAppVersionVersion,mAPILevel, dialogFeedbackBinding.feedbackItemEditText.getText().toString(), mAndroidVersion, mDeviceModel, mDeviceManufacturer, mDeviceName, mNetworkType);;
        onFeedbackSubmitCallback.onFeedbackSubmit(feedback);
        dismiss();
    }

}
