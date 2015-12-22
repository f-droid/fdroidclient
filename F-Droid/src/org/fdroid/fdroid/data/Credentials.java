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

package org.fdroid.fdroid.data;

import java.net.HttpURLConnection;

/**
 * Credentials to authenticate HTTP requests. Implementations if this interface
 * should encapsulate the authentication of an HTTP request in the authenticate
 * method.
 */
public interface Credentials {

    /**
     * Implement this method to provide authentication for the given connection.
     * @param connection the HTTP connection to authenticate
     */
    void authenticate(final HttpURLConnection connection);
}
