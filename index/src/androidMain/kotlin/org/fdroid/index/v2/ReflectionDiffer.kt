package org.fdroid.index.v2

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * A class using Kotlin reflection to implement JSON Merge Patch (RFC 7386) against data classes.
 * This approach loses type-safety, so you need to ensure that the structure of the [JsonObject]
 * matches exactly the structure of the data class you want to apply the diff on.
 * If something unexpected happens, [SerializationException] gets thrown.
 */
public object ReflectionDiffer {

    @Throws(SerializationException::class)
    public fun applyDiff(obj: Map<*, *>, diff: JsonObject): Map<*, *> {
        return getMap(obj, diff)
    }

    @Throws(SerializationException::class)
    public fun <T : Any> applyDiff(obj: T, diff: JsonObject): T {
        val constructor = obj::class.primaryConstructor ?: e("no primary constructor")
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
            params[parameter] = when (prop.returnType.classifier) {
                Int::class -> diff[prop.name]?.jsonPrimitive?.int ?: e("${prop.name} no int")
                Long::class -> diff[prop.name]?.jsonPrimitive?.long ?: e("${prop.name} no long")
                String::class -> diff[prop.name]?.jsonPrimitive?.content
                    ?: e("${prop.name} no string")
                Map::class -> applyDiff(
                    prop.getter.call(obj) as Map<*, *>,
                    diff[prop.name]?.jsonObject ?: e("${prop.name} no map")
                )
                else -> {
                    val newObj = prop.getter.call(obj)
                    val jsonObject = diff[prop.name] as JsonObject
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

    @Throws(SerializationException::class)
    private fun <T : Any> constructFromJson(
        factory: KFunction<T>,
        diff: JsonObject,
    ): T {
        val params = HashMap<KParameter, Any?>()
        factory.parameters.forEach { prop ->
            if (prop.name !in diff) e("${prop.name} required but not found")
            if (diff[prop.name] is JsonNull) {
                if (prop.type.isMarkedNullable) params[prop] = null
                else if (!prop.isOptional) e("not nullable: ${prop.name}")
                return@forEach
            }
            params[prop] = when (prop.type.classifier) {
                Int::class -> diff[prop.name]?.jsonPrimitive?.int ?: e("no int")
                Long::class -> diff[prop.name]?.jsonPrimitive?.long ?: e("no long")
                String::class -> diff[prop.name]?.jsonPrimitive?.content ?: e("no string")
                Map::class -> applyDiff(
                    prop.type::class.createInstance(),
                    diff[prop.name]?.jsonObject!!,
                )
                else -> TODO()
            }
        }
        return factory.callBy(params)
    }

    @Throws(SerializationException::class)
    private fun getMap(map: Map<*, *>, jsonObject: JsonObject?): Map<*, *> {
        return map.toMutableMap().apply {
            jsonObject?.entries?.forEach { (key, value) ->
                if (value is JsonNull) remove(key)
                else set(key, value.jsonPrimitive.content)
            } ?: e("no object")
        }
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
