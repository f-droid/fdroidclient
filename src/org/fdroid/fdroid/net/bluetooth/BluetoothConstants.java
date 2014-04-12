package org.fdroid.fdroid.net.bluetooth;

/**
 * We need some shared information between the client and the server app.
 */
public class BluetoothConstants {

    public static String fdroidUuid() {
        // TODO: Generate a UUID deterministically from, e.g. "org.fdroid.fdroid.net.Bluetooth";
        // This UUID is just from the first example at http://www.ietf.org/rfc/rfc4122.txt
        return "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
    }

}
