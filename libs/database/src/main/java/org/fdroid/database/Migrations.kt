package org.fdroid.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import mu.KotlinLogging

private const val REPO_WEIGHT = 1_000_000_000

internal class MultiRepoMigration : AutoMigrationSpec {

    private val log = KotlinLogging.logger {}

    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        // do migration in one transaction that can be rolled back if there's issues
        db.beginTransaction()
        try {
            migrateWeights(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun migrateWeights(db: SupportSQLiteDatabase) {
        // get repositories
        val repos = ArrayList<Repo>()
        val archiveMap = HashMap<String, Repo>()
        db.query(
            """
            SELECT repoId, address, certificate, weight FROM ${CoreRepository.TABLE}
            JOIN ${RepositoryPreferences.TABLE} USING (repoId)
            ORDER BY weight DESC"""
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val repo = getRepo(cursor)
                log.error { repo.toString() }
                if (repo.isArchive() && repo.certificate != null) {
                    if (archiveMap.containsKey(repo.certificate)) {
                        log.error { "More than two repos with certificate of ${repo.address}" }
                        // still migrating this as a normal repo then
                        repos.add(repo)
                    } else {
                        // remember archive repo, so we get position it below main repo
                        archiveMap[repo.certificate] = repo
                    }
                } else {
                    repos.add(repo)
                }
            }
        }

        // now go through all repos and adapt their weight,
        // so that repos get a higher weight with space for archive repos
        var nextWeight = REPO_WEIGHT
        repos.forEach { repo ->
            val archiveRepo = archiveMap[repo.certificate]
            if (archiveRepo == null) {
                db.updateRepoWeight(repo, nextWeight)
            } else {
                db.updateRepoWeight(repo, nextWeight)
                db.updateRepoWeight(archiveRepo, nextWeight - 1)
                archiveMap.remove(repo.certificate)
            }
            nextWeight -= 2
        }
        // going through archive repos without main repo as well and put them at the end
        // so they don't get stuck with minimum weights
        archiveMap.forEach { (_, repo) ->
            db.updateRepoWeight(repo, nextWeight)
            nextWeight -= 1
        }
    }

    private fun SupportSQLiteDatabase.updateRepoWeight(repo: Repo, newWeight: Int) {
        val rowsAffected = update(
            table = RepositoryPreferences.TABLE,
            conflictAlgorithm = CONFLICT_FAIL,
            values = ContentValues(1).apply {
                put("weight", newWeight)
            },
            whereClause = "repoId = ?",
            whereArgs = arrayOf(repo.repoId),
        )
        if (rowsAffected > 1) error("repo ${repo.address} had more than one preference")
    }

    private fun getRepo(c: Cursor) = Repo(
        repoId = c.getLong(0),
        address = c.getString(1),
        certificate = c.getString(2),
        weight = c.getInt(3),
    )

    private data class Repo(
        val repoId: Long,
        val address: String,
        val certificate: String?,
        val weight: Int,
    ) {
        fun isArchive(): Boolean = address.trimEnd('/').endsWith("/archive")
    }
}
