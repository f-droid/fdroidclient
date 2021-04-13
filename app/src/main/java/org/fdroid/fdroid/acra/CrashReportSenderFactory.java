package org.fdroid.fdroid.acra;

import android.content.Context;

import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

import androidx.annotation.NonNull;

public class CrashReportSenderFactory implements ReportSenderFactory {
    @NonNull
    @Override
    public ReportSender create(@NonNull Context context, @NonNull ACRAConfiguration acraConfiguration) {
        return new CrashReportSender(acraConfiguration);
    }
}
