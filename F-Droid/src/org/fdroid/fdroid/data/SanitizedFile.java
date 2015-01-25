package org.fdroid.fdroid.data;

import java.io.File;

/**
 * File guaranteed to have a santitized name (though not a sanitized path to the parent dir).
 * Useful so that we can use Java's type system to enforce that the file we are accessing
 * doesn't contain illegal characters.
 * Sanitized names are those which only have the following characters: [A-Za-z0-9.-_]
 */
public class SanitizedFile extends File {
    public SanitizedFile(File parent, String name) {
        super(parent, name.replaceAll("[^A-Za-z0-9.-_]", ""));
    }
}
