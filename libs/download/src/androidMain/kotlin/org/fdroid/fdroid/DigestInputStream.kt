/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.fdroid.fdroid

import java.security.DigestInputStream

/**
 * Completes the hash computation by performing final operations such as padding
 * and returns the resulting hash as a hex string.
 * The digest is reset after this call is made,
 * so call this only once and hang on to the result.
 */
public fun DigestInputStream.getDigestHex(): String {
    return messageDigest.digest().toHex()
}
