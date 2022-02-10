package org.fdroid.index.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class IndexV1(
    val repo: RepoV1,
    val requests: Requests,
    val apps: List<AppV1>,
    val packages: Map<String, List<PackageV1>>,
)

@Serializable
public data class RepoV1(
    val timestamp: Long,
    val version: Int,
    @SerialName("maxage")
    val maxAge: Int? = null, // missing in izzy repo
    val name: String,
    val icon: String,
    val address: String,
    val description: String,
    val mirrors: List<String> = emptyList(), // missing in izzy repo
)

@Serializable
public data class Requests(
    val install: List<String>,
    val uninstall: List<String>,
)
