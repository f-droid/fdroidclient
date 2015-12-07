/*
 * Copyright (C) 2015 Christian Morgner (christian.morgner@structr.com)
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

package org.fdroid.fdroid.net.auth;

import android.text.TextUtils;
import java.net.HttpURLConnection;
import org.apache.commons.net.util.Base64;
import org.fdroid.fdroid.data.Credentials;

/**
 * Credentials implementation for HTTP Basic Authentication.
 */
public class HttpBasicCredentials implements Credentials {

    private final String username;
    private final String password;

    public HttpBasicCredentials(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void authenticate(final HttpURLConnection connection) {

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {

            // add authorization header from username / password if set
            connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String((username + ":" + password).getBytes()));
        }
    }
}
