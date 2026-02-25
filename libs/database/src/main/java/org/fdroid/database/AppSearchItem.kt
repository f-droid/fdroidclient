package org.fdroid.database

import androidx.core.os.LocaleListCompat
import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.Relation
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedTextV2

@OptIn(ExperimentalUnsignedTypes::class)
@ConsistentCopyVisibility
public data class AppSearchItem
internal constructor(
  public val repoId: Long,
  public val packageName: String,
  public val lastUpdated: Long,
  public val name: LocalizedTextV2? = null,
  public val summary: LocalizedTextV2? = null,
  public val description: LocalizedTextV2? = null,
  public val authorName: String? = null,
  public val categories: List<String>? = null,
  @Relation(parentColumn = "packageName", entityColumn = "packageName")
  internal val localizedIcon: List<LocalizedIcon>? = null,
  @Suppress("ArrayInDataClass")
  @ColumnInfo("matchinfo(${AppMetadataFts.TABLE}, 'pcx')")
  internal val matchInfo: ByteArray,
) : Comparable<AppSearchItem> {
  public fun getIcon(localeList: LocaleListCompat): FileV2? {
    return localizedIcon
      ?.filter { icon -> icon.repoId == repoId }
      ?.toLocalizedFileV2()
      .getBestLocale(localeList)
  }

  @Ignore public val score: Double

  init {
    val info = matchInfo.toIntArray()
    val numPhrases = info[0]
    val numColumns = info[1]
    val scoreMap = mutableMapOf<Int, Int>()
    for (phrase in 0 until numPhrases) {
      val offset = 2 + phrase * numColumns * 3
      // start with 1 below, because we don't care about repoId column
      for (column in 1 until numColumns) {
        val numHitsInRow = info[offset + 3 * column]
        // increase score if this column had a hit
        if (numHitsInRow > 0) {
          // each hit in a column only contributes to the score once
          scoreMap.getOrPut(column) { weights[column] ?: error("No weight for column $column") }
        }
      }
    }
    val weeksOld = (System.currentTimeMillis() - lastUpdated) / (1000 * 60 * 60 * 24 * 7)
    val punishment = min(100, weeksOld / 3)
    score = scoreMap.values.sum().toDouble() - punishment
  }

  private fun ByteArray.toIntArray(skipSize: Int = 4): IntArray {
    val intArray = IntArray(size / skipSize)
    // go through each 4 bytes to turn them into integers
    (indices step skipSize).forEachIndexed { intIndex, byteIndex ->
      // we are cutting the first two bytes off, because we don't want to deal with UInt
      // and expected integers are small enough
      intArray[intIndex] = ByteBuffer.wrap(this, byteIndex, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }
    return intArray
  }

  override fun compareTo(other: AppSearchItem): Int {
    val scoreComp = score.compareTo(other.score)
    return if (scoreComp == 0) {
      lastUpdated.compareTo(other.lastUpdated)
    } else {
      scoreComp
    }
  }
}

private val weights =
  mapOf(
    // 0 is repoId which we ignore
    1 to 100, // "name"
    2 to 50, // "summary"
    3 to 25, // "description"
    4 to 10, // "authorName"
    5 to 5, // "packageName"
  )
