
package org.fdroid.fdroid.acra;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.acra.ReportField;
import org.acra.config.ConfigUtils;
import org.acra.config.CoreConfiguration;
import org.acra.config.MailSenderConfiguration;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;

import java.util.List;

public class CrashReportSender implements ReportSender {

    private final CoreConfiguration config;
    private final MailSenderConfiguration mailConfig;

    public CrashReportSender(CoreConfiguration config) {
        this.config = config;
        this.mailConfig = ConfigUtils.getPluginConfiguration(config, MailSenderConfiguration.class);
    }

    public void send(@NonNull Context context, @NonNull CrashReportData errorContent) {
        Intent emailIntent = new Intent("android.intent.action.SENDTO");
        emailIntent.setData(Uri.fromParts("mailto", mailConfig.getMailTo(), null));
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String[] subjectBody = this.buildSubjectBody(context, errorContent);
        emailIntent.putExtra("android.intent.extra.SUBJECT", subjectBody[0]);
        emailIntent.putExtra("android.intent.extra.TEXT", subjectBody[1]);
        context.startActivity(emailIntent);
    }

    private String[] buildSubjectBody(Context context, CrashReportData errorContent) {
        List<ReportField> fields = this.config.getReportContent();
        if (fields.isEmpty()) {
            return new String[]{"No ACRA Report Fields found."};
        }

        String subject = context.getPackageName() + " Crash Report";
        StringBuilder builder = new StringBuilder();
        for (ReportField field : fields) {
            builder.append(field.toString()).append('=');
            builder.append(errorContent.getString(field));
            builder.append('\n');
            if ("STACK_TRACE".equals(field.toString())) {
                String stackTrace = errorContent.getString(field);
                if (stackTrace != null) {
                    subject = context.getPackageName() + ": "
                            + stackTrace.substring(0, stackTrace.indexOf('\n'));
                    if (subject.length() > 72) {
                        subject = subject.substring(0, 72);
                    }
                }
            }
        }

        return new String[]{subject, builder.toString()};
    }
}
