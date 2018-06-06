/*
 * Copyright (C) 2016 Blue Jay Wireless
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

package org.fdroid.fdroid.data;

import android.support.annotation.Nullable;

/**
 * Represents action requests embedded in the index XML received from a repo.
 * When {@link #versionCode} is {@code null}, that means that the
 * {@code versionCode} was not specified by the server, and F-Droid should
 * install the best available version.
 */
public class RepoPushRequest {
    public static final String TAG = "RepoPushRequest";

    public static final String INSTALL = "install";
    public static final String UNINSTALL = "uninstall";

    public final String request;
    public final String packageName;
    @Nullable
    public final Integer versionCode;

    public RepoPushRequest(String request, String packageName, @Nullable String versionCode) {
        this.request = request;
        this.packageName = packageName;

        Integer i;
        try {
            i = Integer.parseInt(versionCode);
        } catch (NumberFormatException e) {
            i = null;
        }
        this.versionCode = i;
    }

    @Override
    public String toString() {
        return request + " " + packageName + " " + versionCode;
    }
}
