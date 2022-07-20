package org.fdroid.index.v2

import java.io.InputStream

public interface IndexV2StreamProcessor {
    public fun process(version: Long, inputStream: InputStream, onAppProcessed: (Int) -> Unit)
}
