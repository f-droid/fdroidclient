package org.fdroid.fdroid.acra;

import android.content.Context;

import org.acra.config.CoreConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

import androidx.annotation.NonNull;

public class CrashReportSenderFactory implements ReportSenderFactory {
    @NonNull
    @Override
    public ReportSender create(@NonNull Context context, @NonNull CoreConfiguration coreConfiguration) {
        return new CrashReportSender(coreConfiguration);
    }
}
