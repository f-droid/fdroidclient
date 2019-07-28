/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.installer;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.view.ContextThemeWrapper;

import org.fdroid.fdroid.FDroidApp;

public class ErrorDialogActivity extends FragmentActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String title = intent.getStringExtra(EXTRA_TITLE);
        final String message = intent.getStringExtra(EXTRA_MESSAGE);

        // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        final AlertDialog.Builder builder = new AlertDialog.Builder(theme);
        builder.setTitle(title);
        builder.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(Activity.RESULT_OK);
                        finish();
                    }
                });
        builder.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    }
                });
        builder.setMessage(message);
        builder.create().show();
    }
}
