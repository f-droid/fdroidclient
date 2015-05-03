/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.fdroid.fdroid.R;

import eu.chainfire.libsuperuser.Shell;

public class CheckRootAsyncTask extends AsyncTask<Void, Void, Boolean> {
    ProgressDialog mDialog;
    final Context mContext;
    final CheckRootCallback mCallback;

    public interface CheckRootCallback {
        void onRootCheck(boolean rootGranted);
    }

    public CheckRootAsyncTask(Context context, CheckRootCallback callback) {
        super();
        this.mContext = context;
        this.mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mDialog = new ProgressDialog(mContext);
        mDialog.setTitle(R.string.requesting_root_access_title);
        mDialog.setMessage(mContext.getString(R.string.requesting_root_access_body));
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);
        mDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return Shell.SU.available();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        mDialog.dismiss();

        mCallback.onRootCheck(result);
    }

}
