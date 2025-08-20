package com.metacontent.config

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

object SpawnPermitAdapter : JsonDeserializer<ExcessiveSpawnPermit> {
    override fun deserialize(
        json: JsonElement?,
        typeOf: Type?,
        context: JsonDeserializationContext?
    ): ExcessiveSpawnPermit? {
        val jsonObject = json?.asJsonObject ?: return null
        val type = jsonObject.get("type").asString
        return ExcessiveSpawnPermit.types[type]?.invoke(jsonObject)
    }
}