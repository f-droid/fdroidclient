package org.fdroid.repo

import android.net.Uri
import org.fdroid.database.Repository

internal object RepoUriGetter {

    fun getUri(url: String): NormalizedUri {
        val uri = Uri.parse(url).let {
            when {
                it.scheme.equals("fdroidrepos", ignoreCase = true) -> {
                    it.buildUpon().scheme("https").build()
                }

                it.scheme.equals("fdroidrepo", ignoreCase = true) -> {
                    it.buildUpon().scheme("http").build()
                }

                it.host == "fdroid.link" -> getFdroidLinkUri(it)

                it.scheme.isNullOrBlank() -> {
                    // assume https:// when no scheme given
                    it.buildUpon().scheme("https").path("//${it.path}").build()
                }

                else -> it
            }
        }
        val fingerprint = uri.getQueryParameter("fingerprint")?.lowercase()
            ?: uri.getQueryParameter("FINGERPRINT")?.lowercase()

        val pathSegments = uri.pathSegments
        var username: String? = null
        var password: String? = null
        val normalizedUri = uri.buildUpon().apply {
            // extract and remove userInfo, if available
            val userInfo = uri.userInfo
            val authority = uri.authority
            if (userInfo != null && authority != null) {
                val host = authority.split('@')[1]
                val usernamePassword = userInfo.split(':')
                if (usernamePassword.isNotEmpty()) username = usernamePassword[0]
                if (usernamePassword.size > 1) password = usernamePassword[1]
                authority(host) // remove userInfo from URI
            }
            clearQuery() // removes fingerprint and other query params
            fragment("") // remove # hash fragment
            if (pathSegments.size >= 2 &&
                pathSegments[pathSegments.lastIndex - 1] == "fdroid" &&
                pathSegments.last() == "repo"
            ) {
                // path already is /fdroid/repo, use as is
            } else if (pathSegments.lastOrNull() == "repo") {
                // path already ends in /repo, use as is
            } else if (pathSegments.size >= 1 && pathSegments.last() == "fdroid") {
                // path is /fdroid with missing /repo, so add that
                appendPath("repo")
            } else {
                // path is missing /fdroid/repo, so add it
                appendPath("fdroid")
                appendPath("repo")
            }
        }.build().let { newUri ->
            // hacky way to remove trailing slash
            val path = newUri.path
            if (path != null && path.endsWith('/')) {
                newUri.buildUpon().path(path.trimEnd('/')).build()
            } else {
                newUri
            }
        }
        return NormalizedUri(normalizedUri, fingerprint, username, password)
    }

    fun isSwapUri(uri: Uri): Boolean {
        val swap = uri.getQueryParameter("swap") ?: uri.getQueryParameter("SWAP")
        return swap != null && uri.scheme?.lowercase() == "http"
    }

    private fun getFdroidLinkUri(uri: Uri): Uri {
        val tmpUri = uri.buildUpon().encodedQuery(uri.encodedFragment).build()
        return Uri.parse(tmpUri.getQueryParameter("repo"))
    }

    /**
     * A class for normalizing the [Repository] URI and holding an optional fingerprint
     * as well as username/password for basic authentication.
     */
    data class NormalizedUri(
        val uri: Uri,
        val fingerprint: String?,
        val username: String? = null,
        val password: String? = null,
    )

}
