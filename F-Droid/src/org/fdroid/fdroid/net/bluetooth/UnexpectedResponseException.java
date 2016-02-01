package org.fdroid.fdroid.net.bluetooth;

class UnexpectedResponseException extends Exception {

    UnexpectedResponseException(String message) {
        super(message);
    }

    UnexpectedResponseException(String message, Throwable cause) {
        super("Unexpected response from Bluetooth server: '" + message + "'", cause);
    }
}
