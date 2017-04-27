/*
 * Copyright (C) 2017 Chirayu Desai <chirayudesai1@gmail.com>
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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.fdroid.fdroid.data.Apk;

public class DummyInstaller extends Installer {

    public DummyInstaller(Context context, Apk apk) {
        super(context, apk);
    }

    @Override
    public Intent getPermissionScreen() {
        return null;
    }

    @Override
    public Intent getUninstallScreen() {
        return null;
    }

    @Override
    public void installPackage(Uri localApkUri, Uri downloadUri) {
        // Do nothing
    }

    @Override
    protected void installPackageInternal(Uri localApkUri, Uri downloadUri) {
        // Do nothing
    }

    @Override
    protected void uninstallPackage() {
        // Do nothing
    }

    @Override
    protected boolean isUnattended() {
        return false;
    }

    @Override
    protected boolean supportsContentUri() {
        return false;
    }

}