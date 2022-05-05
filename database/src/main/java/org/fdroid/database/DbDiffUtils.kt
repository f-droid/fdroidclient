package org.fdroid.database

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.fdroid.index.v2.ReflectionDiffer

internal object DbDiffUtils {

    /**
     * Applies the diff from the given [jsonObject] identified by the given [jsonObjectKey]
     * to [itemList] and updates the DB as needed.
     *
     * @param newItem A function to produce a new [T] which typically contains the primary key(s).
     */
    @Throws(SerializationException::class)
    fun <T : Any> diffAndUpdateTable(
        jsonObject: JsonObject,
        jsonObjectKey: String,
        itemList: List<T>,
        itemFinder: (String, T) -> Boolean,
        newItem: (String) -> T,
        deleteAll: () -> Unit,
        deleteOne: (String) -> Unit,
        insertReplace: (List<T>) -> Unit,
        isNewItemValid: (T) -> Boolean = { true },
        keyDenyList: List<String>? = null,
    ) {
        if (!jsonObject.containsKey(jsonObjectKey)) return
        if (jsonObject[jsonObjectKey] == JsonNull) {
            deleteAll()
        } else {
            val obj = jsonObject[jsonObjectKey]?.jsonObject ?: error("no $jsonObjectKey object")
            val list = itemList.toMutableList()
            obj.entries.forEach { (key, value) ->
                if (value is JsonNull) {
                    list.removeAll { itemFinder(key, it) }
                    deleteOne(key)
                } else {
                    value.jsonObject.checkDenyList(keyDenyList)
                    val index = list.indexOfFirst { itemFinder(key, it) }
                    val item = if (index == -1) null else list[index]
                    if (item == null) {
                        val itemToInsert =
                            ReflectionDiffer.applyDiff(newItem(key), value.jsonObject)
                        if (!isNewItemValid(itemToInsert)) throw SerializationException("$newItem")
                        list.add(itemToInsert)
                    } else {
                        list[index] = ReflectionDiffer.applyDiff(item, value.jsonObject)
                    }
                }
            }
            insertReplace(list)
        }
    }

    /**
     * Applies a list diff from a map of lists.
     * The map is identified by the given [jsonObjectKey] in the given [jsonObject].
     * The diff is applied for each key
     * by replacing the existing list using [deleteList] and [insertNewList].
     *
     * @param listParser returns a list of [T] from the given [JsonArray].
     */
    @Throws(SerializationException::class)
    fun <T : Any> diffAndUpdateListTable(
        jsonObject: JsonObject,
        jsonObjectKey: String,
        listParser: (String, JsonArray) -> List<T>,
        deleteAll: () -> Unit,
        deleteList: (String) -> Unit,
        insertNewList: (String, List<T>) -> Unit,
    ) {
        if (!jsonObject.containsKey(jsonObjectKey)) return
        if (jsonObject[jsonObjectKey] == JsonNull) {
            deleteAll()
        } else {
            val obj = jsonObject[jsonObjectKey]?.jsonObject ?: error("no $jsonObjectKey object")
            obj.entries.forEach { (key, list) ->
                if (list is JsonNull) {
                    deleteList(key)
                } else {
                    val newList = listParser(key, list.jsonArray)
                    deleteList(key)
                    insertNewList(key, newList)
                }
            }
        }
    }

    /**
     * Applies the list diff from the given [jsonObject] identified by the given [jsonObjectKey]
     * by replacing an existing list using [deleteList] and [insertNewList].
     *
     * @param listParser returns a list of [T] from the given [JsonArray].
     */
    @Throws(SerializationException::class)
    fun <T : Any> diffAndUpdateListTable(
        jsonObject: JsonObject,
        jsonObjectKey: String,
        listParser: (JsonArray) -> List<T>,
        deleteList: () -> Unit,
        insertNewList: (List<T>) -> Unit,
    ) {
        if (!jsonObject.containsKey(jsonObjectKey)) return
        if (jsonObject[jsonObjectKey] == JsonNull) {
            deleteList()
        } else {
            val jsonArray = jsonObject[jsonObjectKey]?.jsonArray ?: error("no $jsonObjectKey array")
            val list = listParser(jsonArray)
            deleteList()
            insertNewList(list)
        }
    }

    private fun JsonObject.checkDenyList(list: List<String>?) {
        list?.forEach { forbiddenKey ->
            if (containsKey(forbiddenKey)) throw SerializationException(forbiddenKey)
        }
    }

}
