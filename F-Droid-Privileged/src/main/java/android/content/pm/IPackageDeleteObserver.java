/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Just a non-working implementation of this Stub to satisfy compiler!
 */
public interface IPackageDeleteObserver extends IInterface {

    abstract class Stub extends Binder implements android.content.pm.IPackageDeleteObserver {

        public Stub() {
            throw new RuntimeException("Stub!");
        }

        public static IPackageDeleteObserver asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }

        public IBinder asBinder() {
            throw new RuntimeException("Stub!");
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            throw new RuntimeException("Stub!");
        }
    }

    void packageDeleted(java.lang.String packageName, int returnCode) throws RemoteException;
}
