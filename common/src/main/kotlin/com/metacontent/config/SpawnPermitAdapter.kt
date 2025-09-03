package com.metacontent.config

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

object SpawnPermitAdapter : JsonDeserializer<ExcessiveSpawnPermit>, JsonSerializer<ExcessiveSpawnPermit> {
    override fun deserialize(
        json: JsonElement?,
        typeOf: Type?,
        context: JsonDeserializationContext?
    ): ExcessiveSpawnPermit? {
        val jsonObject = json?.asJsonObject ?: return null
        val type = jsonObject.get("type")?.asString
        return ExcessiveSpawnPermit.types[type]?.deserializer(jsonObject)
    }

    override fun serialize(
        src: ExcessiveSpawnPermit?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement? {
        if (src == null) return null

        val serializer = ExcessiveSpawnPermit.types[src.type]?.serializer
        return if (serializer != null) {
            val json = JsonObject()
            json.addProperty("type", src.type)
            src.max?.let { json.addProperty("max", it) }
            serializer(src, json)
        } else {
            context?.serialize(src)
        }
    }
}