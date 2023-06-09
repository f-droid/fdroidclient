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

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import org.fdroid.fdroid.R;

public class ErrorDialogActivity extends FragmentActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String title = intent.getStringExtra(EXTRA_TITLE);
        final String message = intent.getStringExtra(EXTRA_MESSAGE);

        // pass the theme, it is not automatically applied due to activity's Theme.NoDisplay
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_App);
        builder.setTitle(title);
        builder.setNeutralButton(android.R.string.ok, (dialog, which) -> {
            setResult(AppCompatActivity.RESULT_OK);
            finish();
        });
        builder.setOnCancelListener(dialog -> {
            setResult(AppCompatActivity.RESULT_CANCELED);
            finish();
        });
        builder.setMessage(message);
        builder.create().show();
    }
}
