package org.fdroid.fdroid.acra;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import org.acra.dialog.BaseCrashReportDialog;
import org.fdroid.fdroid.R;

public class CrashReportActivity extends BaseCrashReportDialog
        implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener {

    private static final String STATE_COMMENT = "comment";
    private EditText comment;

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.crash_dialog_title)
                .setView(R.layout.crash_report_dialog)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(this);
        dialog.show();

        comment = (EditText) dialog.findViewById(android.R.id.input);
        if (savedInstanceState != null) {
            comment.setText(savedInstanceState.getString(STATE_COMMENT));
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            sendCrash(comment.getText().toString(), "");
        } else {
            cancelReports();
        }
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_COMMENT, comment.getText().toString());
        super.onSaveInstanceState(outState);
    }
}
