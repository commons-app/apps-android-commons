package fr.free.nrw.commons.logging;

import android.content.Context;
import android.support.annotation.NonNull;

import org.acra.config.CoreConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.ApplicationlessInjection;

public class CommonsLogSenderFactory implements ReportSenderFactory {

    @Inject
    SessionManager sessionManager;

    public CommonsLogSenderFactory() {

    }

    @NonNull
    @Override
    public ReportSender create(@NonNull Context context, @NonNull CoreConfiguration config) {
        ApplicationlessInjection
                .getInstance(context)
                .getCommonsApplicationComponent()
                .inject(this);

        return new CommonsLogSender(sessionManager);
    }

    @Override
    public boolean enabled(@Nonnull CoreConfiguration coreConfig) {
        return true;
    }
}