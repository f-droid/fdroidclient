/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import java.util.ArrayList;
import java.util.Arrays;

public class FDroidCertPins {
    public static final String[] DEFAULT_PINS =
    {
        /*
         * SubjectDN: CN=f-droid.org, OU=PositiveSSL, OU=Domain Control Validated
         * IssuerDN: CN=PositiveSSL CA 2, O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB
         * Fingerprint: 84B91CDF2312CB9BA7F3BE803783302F8D8C299F
         * SPKI Pin: 638F93856E1F5EDFCBD40C46D4160CFF21B0713A
         */
        "638F93856E1F5EDFCBD40C46D4160CFF21B0713A",

        /*
         * SubjectDN: CN=guardianproject.info, OU=Gandi Standard SSL, OU=Domain Control Validated
         * IssuerDN: CN=Gandi Standard SSL CA, O=GANDI SAS, C=FR
         * Fingerprint: 187C2573E924DFCBFF2A781A2F99D71C6E031828
         * SPKI Pin: EB6BBC6C6BAEEA20CB0F3357720D86E0F3A526F4
         */
        "EB6BBC6C6BAEEA20CB0F3357720D86E0F3A526F4",
    };

    public static ArrayList<String> PINLIST = null;

    public static String[] getPinList()
    {
        if(PINLIST == null)
        {
            PINLIST = new ArrayList<String>();
            PINLIST.addAll(Arrays.asList(DEFAULT_PINS));
        }

        return PINLIST.toArray(new String[PINLIST.size()]);
    }
}
