/*
 * Copyright (C) 2016 Blue Jay Wireless
 * Copyright (C) 2014-2016 Daniel Martí <mvdan@mvdan.cc>
 * Copyright (C) 2014-2016 Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2014-2016 Peter Serwylo <peter@serwylo.com>
 * Copyright (C) 2015 Christian Morgner
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

import android.net.Uri;
import org.fdroid.fdroid.net.TreeUriDownloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a the descriptive info and metadata about a given repo, as provided
 * by the repo index.  This also keeps track of the state of the repo.
 * <p>
 * <b>Do not rename these instance variables without careful consideration!</b>
 * They are mapped to JSON field names, the {@code fdroidserver} internal variable
 * names, and the {@code fdroiddata} YAML field names.  Only the instance variables
 * decorated with {@code @JsonIgnore} are not directly mapped.
 *
 * @see <a href="https://gitlab.com/fdroid/fdroiddata">fdroiddata</a>
 * @see <a href="https://gitlab.com/fdroid/fdroidserver">fdroidserver</a>
 */
public class Repo {

    public static final int PUSH_REQUEST_IGNORE = 0;

    public String address;

    @Deprecated // not taking mirrors into account
    public String getFileUrl(String... pathElements) {
        /* Each String in pathElements might contain a /, should keep these as path elements */
        List<String> elements = new ArrayList<>();
        for (String element : pathElements) {
            Collections.addAll(elements, element.split("/"));
        }

        /*
         * Storage Access Framework URLs have this wacky URL-encoded path within the URL path.
         *
         * i.e.
         * content://authority/tree/313E-1F1C%3A/document/313E-1F1C%3Aguardianproject.info%2Ffdroid%2Frepo
         *
         * Currently don't know a better way to identify these than by content:// prefix,
         * seems the Android SDK expects apps to consider them as opaque identifiers.
         */
        if (address.startsWith("content://")) {
            StringBuilder result = new StringBuilder(address);
            for (String element : elements) {
                result.append(TreeUriDownloader.ESCAPED_SLASH);
                result.append(element);
            }
            return result.toString();
        } else { // Normal URL
            Uri.Builder result = Uri.parse(address).buildUpon();
            for (String element : elements) {
                result.appendPath(element);
            }
            return result.build().toString();
        }
    }
}
