package org.fdroid.fdroid.acra;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

import org.acra.dialog.CrashReportDialog;
import org.acra.dialog.CrashReportDialogHelper;
import org.fdroid.fdroid.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

public class CrashReportActivity extends CrashReportDialog
        implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener {

    private static final String STATE_COMMENT = "comment";
    private CrashReportDialogHelper helper;
    private EditText comment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new CrashReportDialogHelper(this, getIntent());
        if (savedInstanceState != null) {
            comment.setText(savedInstanceState.getString(STATE_COMMENT));
        }
    }

    @Override
    protected void setDialog(@NonNull android.app.AlertDialog alertDialog) {
        super.setDialog(alertDialog);
    }

    @NonNull
    @Override
    protected android.app.AlertDialog getDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.crash_dialog_title)
                .setView(R.layout.crash_report_dialog)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(this);
        dialog.setOnShowListener(d -> {
            TextInputLayout commentLayout = dialog.findViewById(android.R.id.input);
            comment = commentLayout.getEditText();
        });
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onClick(@NonNull DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            helper.sendCrash(comment.getText().toString(), "");
        } else {
            helper.cancelReports();
        }
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_COMMENT, comment.getText().toString());
        super.onSaveInstanceState(outState);
    }
}
