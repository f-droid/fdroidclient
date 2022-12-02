package org.fdroid.index.v2

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

/**
 * A class using Kotlin reflection to implement JSON Merge Patch (RFC 7386) against data classes.
 * This approach loses type-safety, so you need to ensure that the structure of the [JsonObject]
 * matches exactly the structure of the data class you want to apply the diff on.
 * If something unexpected happens, [SerializationException] gets thrown.
 */
public object ReflectionDiffer {

    @Throws(SerializationException::class)
    public fun <T : Any> applyDiff(obj: T, diff: JsonObject): T {
        val constructor = obj::class.primaryConstructor ?: e("no primary constructor ${obj::class}")
        val params = HashMap<KParameter, Any?>()
        constructor.parameters.forEach { parameter ->
            val prop = obj::class.memberProperties.find { memberProperty ->
                memberProperty.name == parameter.name
            } ?: e("no member property for constructor, is data class?")
            if (prop.name !in diff) {
                params[parameter] = prop.getter.call(obj)
                return@forEach
            }
            if (diff[prop.name] is JsonNull) {
                if (parameter.type.isMarkedNullable) params[parameter] = null
                else if (!parameter.isOptional) e("not nullable: ${parameter.name}")
                return@forEach
            }
            @Suppress("UNCHECKED_CAST")
            params[parameter] = when (prop.returnType.classifier) {
                Int::class -> diff[prop.name]?.primitiveOrNull()?.intOrNull
                    ?: e("${prop.name} no int")
                Long::class -> diff[prop.name]?.primitiveOrNull()?.longOrNull
                    ?: e("${prop.name} no long")
                String::class -> diff[prop.name]?.primitiveOrNull()?.contentOrNull
                    ?: e("${prop.name} no string")
                List::class -> diff[prop.name]?.jsonArrayOrNull()?.map {
                    it.primitiveOrNull()?.contentOrNull ?: e("${prop.name} non-primitive array")
                } ?: e("${prop.name} no array")
                Map::class -> diffMap(prop.returnType, prop.getter.call(obj), prop.name, diff)
                else -> {
                    val newObj = prop.getter.call(obj)
                    val jsonObject = diff[prop.name] as? JsonObject ?: e("${prop.name} no dict")
                    if (newObj == null) {
                        val factory = (prop.returnType.classifier as KClass<*>).primaryConstructor!!
                        constructFromJson(factory, jsonObject)
                    } else {
                        applyDiff(newObj, jsonObject)
                    }
                }
            }
        }
        return constructor.callBy(params)
    }

    /**
     * Used when the diff introduces a new object.
     * As the object did not exist before, we can not apply a diff to it,
     * but must construct it from scratch.
     * We use the given [factory] for that which is typically a constructor of the object <T>.
     */
    @Throws(SerializationException::class)
    internal fun <T : Any> constructFromJson(
        factory: KFunction<T>,
        diff: JsonObject,
    ): T {
        val params = HashMap<KParameter, Any?>()
        factory.parameters.forEach { prop ->
            if (prop.name !in diff) {
                if (prop.isOptional) return@forEach
                else e("${prop.name} required but not found")
            }
            if (diff[prop.name] is JsonNull) {
                if (prop.type.isMarkedNullable) params[prop] = null
                else if (!prop.isOptional) e("not nullable: ${prop.name}")
                return@forEach
            }
            params[prop] = when (prop.type.classifier) {
                Int::class -> diff[prop.name]?.primitiveOrNull()?.intOrNull
                    ?: e("${prop.name} no int")
                Long::class -> diff[prop.name]?.primitiveOrNull()?.longOrNull
                    ?: e("${prop.name} no long")
                String::class -> diff[prop.name]?.primitiveOrNull()?.contentOrNull
                    ?: e("${prop.name} no string")
                List::class -> diff[prop.name]?.jsonArrayOrNull()?.map {
                    it.primitiveOrNull()?.contentOrNull ?: e("${prop.name} non-primitive array")
                } ?: e("${prop.name} no array")
                Map::class -> diffMap(prop.type, null, prop.name, diff)
                else -> constructFromJson(
                    factory = (prop.type.classifier as KClass<*>).primaryConstructor!!,
                    diff = diff[prop.name]?.jsonObjectOrNull() ?: e("${prop.name} no dict"),
                )
            }
        }
        return factory.callBy(params)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> diffMap(type: KType, obj: T?, key: String?, diff: JsonObject) = when (type) {
        typeOf<LocalizedTextV2>() -> applyTextDiff(
            obj = obj as? LocalizedTextV2 ?: HashMap(),
            diff = diff[key]?.jsonObjectOrNull() ?: e("$key no map"),
        )
        typeOf<LocalizedTextV2?>() -> applyTextDiff(
            obj = obj as? LocalizedTextV2? ?: HashMap(),
            diff = diff[key]?.jsonObjectOrNull() ?: e("$key no map"),
        )
        typeOf<LocalizedFileV2>() -> applyFileDiff(
            obj = obj as? LocalizedFileV2 ?: HashMap(),
            diff = diff[key]?.jsonObjectOrNull() ?: e("$key no map"),
        )
        typeOf<LocalizedFileV2?>() -> applyFileDiff(
            obj = obj as? LocalizedFileV2? ?: HashMap(),
            diff = diff[key]?.jsonObjectOrNull() ?: e("$key no map"),
        )
        typeOf<Map<String, LocalizedTextV2>>() -> applyMapTextDiff(
            obj = obj as? Map<String, LocalizedTextV2> ?: HashMap(),
            diff = diff[key]?.jsonObjectOrNull() ?: e("$key no map"),
        )
        typeOf<Map<String, LocalizedTextV2>?>() -> applyMapTextDiff(
            obj = obj as? Map<String, LocalizedTextV2>? ?: HashMap(),
            diff = diff[key]?.jsonObjectOrNull() ?: e("$key no map"),
        )
        else -> e("Unknown map: $key: $type = ${diff[key]}")
    }

    @Throws(SerializationException::class)
    private fun applyTextDiff(
        obj: LocalizedTextV2,
        diff: JsonObject,
    ): LocalizedTextV2 = obj.toMutableMap().apply {
        diff.entries.forEach { (locale, textElement) ->
            if (textElement is JsonNull) {
                remove(locale)
                return@forEach
            }
            val text = textElement.primitiveOrNull()?.contentOrNull
                ?: throw SerializationException("no string: $textElement")
            set(locale, text)
        }
    }

    @Throws(SerializationException::class)
    private fun applyFileDiff(
        obj: LocalizedFileV2,
        diff: JsonObject,
    ): LocalizedFileV2 = obj.toMutableMap().apply {
        diff.entries.forEach { (locale, fileV2Element) ->
            if (fileV2Element is JsonNull) {
                remove(locale)
                return@forEach
            }
            val fileV2Object = fileV2Element.jsonObjectOrNull()
                ?: throw SerializationException("no FileV2: $fileV2Element")
            val fileV2 = if (locale in obj) {
                applyDiff(obj[locale] as FileV2, fileV2Object)
            } else {
                constructFromJson(FileV2::class.primaryConstructor!!, fileV2Object)
            }
            set(locale, fileV2)
        }
    }

    @Throws(SerializationException::class)
    private fun applyMapTextDiff(
        obj: Map<String, LocalizedTextV2>,
        diff: JsonObject,
    ): Map<String, LocalizedTextV2> = obj.toMutableMap().apply {
        diff.entries.forEach { (key, localizedTextElement) ->
            if (localizedTextElement is JsonNull) {
                remove(key)
                return@forEach
            }
            val localizedTextObject = localizedTextElement.jsonObjectOrNull()
                ?: throw SerializationException("no FileV2: $localizedTextElement")
            val localizedText = if (key in obj) {
                applyTextDiff(obj[key] as LocalizedTextV2, localizedTextObject)
            } else {
                applyTextDiff(HashMap(), localizedTextObject)
            }
            set(key, localizedText)
        }
    }

    private fun JsonElement.primitiveOrNull(): JsonPrimitive? = try {
        jsonPrimitive
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun JsonElement.jsonArrayOrNull(): JsonArray? = try {
        jsonArray
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun JsonElement.jsonObjectOrNull(): JsonObject? = try {
        jsonObject
    } catch (e: IllegalArgumentException) {
        null
    }

    @Throws(SerializationException::class)
    private fun e(msg: String): Nothing = throw SerializationException(msg)

    public inline fun <reified T> Json.decodeOr(
        key: String,
        json: JsonObject,
        default: () -> T,
    ): T {
        return if (json.containsKey(key)) {
            decodeFromJsonElement(serializersModule.serializer(), json)
        } else {
            default()
        }
    }

}
