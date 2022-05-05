package org.fdroid.database

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import kotlinx.serialization.json.JsonObject
import org.fdroid.CompatibilityChecker
import org.fdroid.index.v2.IndexV2DiffStreamReceiver

internal class DbV2DiffStreamReceiver(
    private val db: FDroidDatabaseInt,
    private val repoId: Long,
    private val compatibilityChecker: CompatibilityChecker,
) : IndexV2DiffStreamReceiver {

    private val locales: LocaleListCompat = getLocales(Resources.getSystem().configuration)

    override fun receiveRepoDiff(repoJsonObject: JsonObject) {
        db.getRepositoryDao().updateRepository(repoId, repoJsonObject)
    }

    override fun receivePackageMetadataDiff(packageId: String, packageJsonObject: JsonObject?) {
        db.getAppDao().updateApp(repoId, packageId, packageJsonObject, locales)
    }

    override fun receiveVersionsDiff(
        packageId: String,
        versionsDiffMap: Map<String, JsonObject?>?,
    ) {
        db.getVersionDao().update(repoId, packageId, versionsDiffMap) {
            compatibilityChecker.isCompatible(it)
        }
    }

    @Synchronized
    override fun onStreamEnded() {
        db.afterUpdatingRepo(repoId)
    }

}
