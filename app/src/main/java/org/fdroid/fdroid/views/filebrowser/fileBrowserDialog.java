package org.fdroid.fdroid.views.filebrowser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;


public class fileBrowserDialog {

    private Context context;
    private final String TAG = "fileBrowserDialog";


    //constructor
    public fileBrowserDialog(Context context) {
        this.context = context;
    }


    public void result(int requestCode, int buttonValue, String text, Object passOn) {
        //Return Values
    }


    public void open(
            final int requestCode,
            int[] button,
            String[] buttonValue,
            String title,
            String message,
            final String inputText,
            Object passOn) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        if (title != null) {
            builder.setTitle(title);
        }

        if (message != null) {
            builder.setMessage(message);
        }

        final EditText input = new EditText(context);
        if (inputText != null) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
            input.setText(inputText);
            builder.setView(input);
        }

        final Object finalPassOn = passOn;
        DialogInterface.OnClickListener listener_Button = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (inputText != null) {
                    result(requestCode, i, input.getText().toString(), finalPassOn);
                } else {
                    result(requestCode, i, null, finalPassOn);
                }
            }
        };

        for (int i = 0; i < button.length; i++) {
            if (button[i] == AlertDialog.BUTTON_POSITIVE) {
                builder.setPositiveButton(buttonValue[i], listener_Button);
            } else if (button[i] == AlertDialog.BUTTON_NEUTRAL) {
                builder.setNeutralButton(buttonValue[i], listener_Button);
            } else if (button[i] == AlertDialog.BUTTON_NEGATIVE) {
                builder.setNegativeButton(buttonValue[i], listener_Button);
            }
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
