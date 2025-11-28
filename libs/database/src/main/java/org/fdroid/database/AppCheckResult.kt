package org.fdroid.database

public data class AppCheckResult(
    val updates: List<UpdatableApp>,
    val issues: List<AppWithIssue>,
)

public sealed interface AppWithIssue {
    public val packageName: String
    public val installVersionName: String
    public val issue: AppIssue
}

public data class AvailableAppWithIssue(
    val app: AppOverviewItem,
    override val installVersionName: String,
    val installVersionCode: Long,
    override val issue: AppIssue,
) : AppWithIssue {
    override val packageName: String = app.packageName
}

public data class UnavailableAppWithIssue(
    override val packageName: String,
    val name: CharSequence?,
    override val installVersionName: String,
    val installVersionCode: Long,
) : AppWithIssue {
    override val issue: AppIssue = NotAvailable
}

public sealed interface AppIssue

/**
 * An app that we installed in the past, but is no longer available in any (enabled) repository.
 */
public data object NotAvailable : AppIssue

/**
 * An app that can not get updated, because all versions have an incompatible signer.
 * There may be compatible versions in another repo.
 */
public data class NoCompatibleSigner(val repoIdWithCompatibleSigner: Long? = null) : AppIssue

/**
 * An app that could get updated, but only from another repo.
 */
public data class UpdateInOtherRepo(val repoIdWithUpdate: Long) : AppIssue

/**
 * Has a known vulnerability and should either get updated or uninstalled.
 * @param fromPreferredRepo true if the preferred repo had marked the app with known vulnerability.
 */
public data class KnownVulnerability(val fromPreferredRepo: Boolean) : AppIssue
