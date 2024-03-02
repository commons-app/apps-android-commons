package fr.free.nrw.commons.feedback;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
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
        dialogFeedbackBinding.btnSubmitFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitFeedback();
            }
        });
    }

    /**
     * When the button is clicked, it will create a feedback object
     * and give a callback to calling activity/fragment
     */
    void submitFeedback() {
        if(dialogFeedbackBinding.feedbackItemEditText.getText().toString().equals("")) {
            dialogFeedbackBinding.feedbackItemEditText.setError(getContext().getString(R.string.enter_description));
            return;
        }
        String appVersion = ConfigUtils.getVersionNameWithSha(getContext());
        String androidVersion = dialogFeedbackBinding.androidVersionCheckbox.isChecked() ? DeviceInfoUtil.getAndroidVersion() : null;
        String apiLevel = dialogFeedbackBinding.apiLevelCheckbox.isChecked() ? DeviceInfoUtil.getAPILevel() : null;
        String deviceManufacturer = dialogFeedbackBinding.deviceManufacturerCheckbox.isChecked() ? DeviceInfoUtil.getDeviceManufacturer() : null;
        String deviceModel = dialogFeedbackBinding.deviceModelCheckbox.isChecked() ? DeviceInfoUtil.getDeviceModel() : null;
        String deviceName = dialogFeedbackBinding.deviceNameCheckbox.isChecked() ? DeviceInfoUtil.getDevice() : null;
        String networkType = dialogFeedbackBinding.networkTypeCheckbox.isChecked() ? DeviceInfoUtil.getConnectionType(getContext()).toString() : null;
        Feedback feedback = new Feedback(appVersion, apiLevel
            , dialogFeedbackBinding.feedbackItemEditText.getText().toString()
            , androidVersion, deviceModel, deviceManufacturer, deviceName, networkType);
        onFeedbackSubmitCallback.onFeedbackSubmit(feedback);
        dismiss();
    }

}
