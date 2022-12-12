package org.fdroid.fdroid.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.InitialRepository

object ContentProviderMigrator {

    private const val OLD_DB_NAME = "fdroid"

    fun needsMigration(context: Context): Boolean {
        return context.databaseList().contains(OLD_DB_NAME)
    }

    fun migrateOldRepos(context: Context, db: FDroidDatabase) {
        val repoDao = db.getRepositoryDao()
        val repos = repoDao.getRepositories()
        var weight = repos.last().weight
        ContentProviderDbHelper(context).readableDatabase.use { oldDb ->
            val projection = arrayOf("address", "pubkey", "inuse", "userMirrors", "disabledMirrors")
            oldDb.query("fdroid_repo", projection, null, null, null, null, null).use { c ->
                while (c.moveToNext()) {
                    val address = c.getString(c.getColumnIndexOrThrow("address"))
                    val certificate = c.getString(c.getColumnIndexOrThrow("pubkey"))
                        ?.lowercase() ?: continue
                    val enabled = c.getInt(c.getColumnIndexOrThrow("inuse")) == 1
                    val userMirrors = c.getString(c.getColumnIndexOrThrow("userMirrors"))
                        ?.split(',')
                    val disabledMirrors = c.getString(c.getColumnIndexOrThrow("disabledMirrors"))
                        ?.split(',')
                    // find existing repos by address, because F-Droid archive re-uses certificate
                    val repo = repos.find { it.address == address }
                    if (repo == null) { // new repo to be added to new DB
                        val newRepo = InitialRepository(
                            name = "",
                            address = address,
                            description = "",
                            certificate = certificate,
                            version = 0,
                            enabled = enabled,
                            weight = ++weight,
                        )
                        repoDao.insert(newRepo)
                    } else { // old repo that may need an update for the new DB
                        if (repo.certificate != certificate) {
                            continue // don't update with certificate does not match
                        }
                        if (repo.enabled != enabled) {
                            repoDao.setRepositoryEnabled(repo.repoId, enabled)
                        }
                        if (!userMirrors.isNullOrEmpty()) {
                            repoDao.updateUserMirrors(repo.repoId, userMirrors)
                        }
                        if (!disabledMirrors.isNullOrEmpty()) {
                            repoDao.updateDisabledMirrors(repo.repoId, disabledMirrors)
                        }
                    }
                }
            }
        }
    }

    fun removeOldDb(context: Context) {
        context.deleteDatabase(OLD_DB_NAME)
    }

    private class ContentProviderDbHelper(
        context: Context,
    ) : SQLiteOpenHelper(context, OLD_DB_NAME, null, 85) {
        override fun onCreate(db: SQLiteDatabase) {}
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
}
