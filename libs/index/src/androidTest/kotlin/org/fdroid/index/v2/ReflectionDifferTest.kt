package org.fdroid.index.v2

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.fdroid.index.IndexParser
import org.fdroid.index.IndexParser.json
import org.fdroid.index.assetPath
import org.fdroid.index.parseV2
import org.fdroid.test.DiffUtils.clean
import org.fdroid.test.DiffUtils.cleanMetadata
import org.fdroid.test.DiffUtils.cleanRepo
import org.fdroid.test.DiffUtils.cleanVersion
import org.fdroid.test.LOCALE
import java.io.File
import java.io.FileInputStream
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ReflectionDifferTest {

    @Test
    fun testEmptyToMin() = testDiff(
        diffPath = "$assetPath/diff-empty-min/23.json",
        startPath = "$assetPath/index-empty-v2.json",
        endPath = "$assetPath/index-min-v2.json",
    )

    @Test
    fun testEmptyToMid() = testDiff(
        diffPath = "$assetPath/diff-empty-mid/23.json",
        startPath = "$assetPath/index-empty-v2.json",
        endPath = "$assetPath/index-mid-v2.json",
    )

    @Test
    fun testEmptyToMax() = testDiff(
        diffPath = "$assetPath/diff-empty-max/23.json",
        startPath = "$assetPath/index-empty-v2.json",
        endPath = "$assetPath/index-max-v2.json",
    )

    @Test
    fun testMinToMid() = testDiff(
        diffPath = "$assetPath/diff-empty-mid/42.json",
        startPath = "$assetPath/index-min-v2.json",
        endPath = "$assetPath/index-mid-v2.json",
    )

    @Test
    fun testMinToMax() = testDiff(
        diffPath = "$assetPath/diff-empty-max/42.json",
        startPath = "$assetPath/index-min-v2.json",
        endPath = "$assetPath/index-max-v2.json",
    )

    @Test
    fun testMidToMax() = testDiff(
        diffPath = "$assetPath/diff-empty-max/1337.json",
        startPath = "$assetPath/index-mid-v2.json",
        endPath = "$assetPath/index-max-v2.json",
    )

    @Test
    fun testLocalizedFileV2() {
        val category1 = CategoryV2(
            name = mapOf(LOCALE to "Cat1"),
            icon = mapOf(LOCALE to FileV2(
                name = "file1",
                sha256 = "hash",
                size = 1,
            )),
        )
        val category2 = CategoryV2(
            name = mapOf(LOCALE to "Cat2"),
        )
        val diff1 = JsonObject(
            mapOf("icon" to JsonObject(
                mapOf(LOCALE to JsonObject(
                    mapOf("name" to JsonPrimitive("file1b"))
                ))
            ))
        )
        val diff2 = JsonObject(
            mapOf("icon" to JsonObject(
                mapOf(LOCALE to JsonObject(
                    mapOf(
                        "name" to JsonPrimitive("file2"),
                        "sha256" to JsonPrimitive("hash2"),
                        "size" to JsonPrimitive(2L),
                    )
                ))
            ))
        )
        val diffedCat1 = ReflectionDiffer.applyDiff(category1, diff1)
        val diffedCat2 = ReflectionDiffer.applyDiff(category2, diff2)
        val diffedIcon1 = diffedCat1.icon[LOCALE]
        val diffedIcon2 = diffedCat2.icon[LOCALE]
        val expectedIcon1 = FileV2(
            name = "file1b",
            sha256 = "hash",
            size = 1,
        )
        val expectedIcon2 = FileV2(
            name = "file2",
            sha256 = "hash2",
            size = 2,
        )
        assertEquals(expectedIcon1, diffedIcon1)
        assertEquals(expectedIcon2, diffedIcon2)
    }

    @Test
    fun testClassWithoutPrimaryConstructor() {
        class NoConstructor {
            @Suppress("ConvertSecondaryConstructorToPrimary", "UNUSED_PARAMETER")
            constructor(i: Int)
        }
        assertFailsWith<SerializationException> {
            ReflectionDiffer.applyDiff(NoConstructor(0), JsonObject(emptyMap()))
        }.also { assertContains(it.message!!, "no primary constructor") }
    }

    @Test
    fun testNoMemberForConstructorParameter() {
        @Suppress("UNUSED_PARAMETER")
        class NoConstructor(i: Int)
        assertFailsWith<SerializationException> {
            ReflectionDiffer.applyDiff(NoConstructor(0), JsonObject(emptyMap()))
        }.also { assertContains(it.message!!, "no member property for constructor") }
    }

    @Test
    fun testNullingRequiredParameter() {
        data class Required(val test: String)
        assertFailsWith<SerializationException> {
            ReflectionDiffer.applyDiff(
                Required("foo"),
                JsonObject(mapOf("test" to JsonNull))
            )
        }.also { assertContains(it.message!!, "not nullable: test") }
    }

    @Test
    fun testWrongTypes() {
        data class Types(val str: String? = null, val i: Int? = null, val l: Long? = null)

        // string as object
        assertFailsWith<SerializationException> {
            ReflectionDiffer.applyDiff(
                Types(str = "foo"),
                JsonObject(mapOf("str" to JsonObject(emptyMap())))
            )
        }.also { assertContains(it.message!!, "str no string") }

        // int as string
        assertFailsWith<SerializationException> {
            ReflectionDiffer.applyDiff(
                Types(i = 23),
                JsonObject(mapOf("i" to JsonPrimitive("test")))
            )
        }.also { assertContains(it.message!!, "i no int") }

        // int as long
        assertFailsWith<SerializationException> {
            ReflectionDiffer.applyDiff(
                Types(i = 23),
                JsonObject(mapOf("i" to JsonPrimitive(Long.MAX_VALUE)))
            )
        }.also { assertContains(it.message!!, "i no int") }

        // long as array
        assertFailsWith<SerializationException> {
            ReflectionDiffer.applyDiff(
                Types(l = 23L),
                JsonObject(mapOf("l" to JsonArray(emptyList())))
            )
        }.also { assertContains(it.message!!, "l no long") }
    }

    private fun testDiff(diffPath: String, startPath: String, endPath: String) {
        val diffFile = File(diffPath)
        val startFile = File(startPath)
        val endFile = File(endPath)
        val diff = json.parseToJsonElement(diffFile.readText()).jsonObject
        val start = IndexParser.parseV2(FileInputStream(startFile))
        val end = IndexParser.parseV2(FileInputStream(endFile))

        // diff repo
        val repoJson = diff["repo"]!!.jsonObject.cleanRepo()
        val repo: RepoV2 = ReflectionDiffer.applyDiff(start.repo.clean(), repoJson)
        assertEquals(end.repo.clean(), repo)
        // apply diff to all start packages present in end index
        end.packages.forEach packages@{ (packageName, packageV2) ->
            val packageDiff = diff["packages"]?.jsonObject?.get(packageName)?.jsonObject
                ?: return@packages
            // apply diff to metadata
            val metadataDiff = packageDiff.jsonObject["metadata"]?.jsonObject?.cleanMetadata()
            if (metadataDiff != null) {
                val startMetadata = start.packages[packageName]?.metadata?.clean() ?: run {
                    val factory = MetadataV2::class.primaryConstructor!!
                    ReflectionDiffer.constructFromJson(factory, metadataDiff)
                }
                val metadataV2: MetadataV2 = ReflectionDiffer.applyDiff(startMetadata, metadataDiff)
                assertEquals(packageV2.metadata.clean(), metadataV2)
            }
            // apply diff to all start versions present in end index
            packageV2.versions.forEach versions@{ (versionId, packageVersionV2) ->
                val versionsDiff = packageDiff.jsonObject["versions"]?.jsonObject
                    ?.get(versionId)?.jsonObject?.cleanVersion() ?: return@versions
                val startVersion = start.packages[packageName]?.versions?.get(versionId)?.clean()
                    ?: run {
                        val factory = PackageVersionV2::class.primaryConstructor!!
                        ReflectionDiffer.constructFromJson(factory, versionsDiff)
                    }
                val version: PackageVersionV2 =
                    ReflectionDiffer.applyDiff(startVersion, versionsDiff)
                assertEquals(packageVersionV2.clean(), version)
            }
        }
    }

}
