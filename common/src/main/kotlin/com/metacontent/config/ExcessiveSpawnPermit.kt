package com.metacontent.config

import com.cobblemon.mod.common.api.spawning.SpawnCause
import com.cobblemon.mod.common.util.getPlayer
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation

abstract class ExcessiveSpawnPermit() {
    companion object {
        internal val types = mutableMapOf<String, PermitAdapter>()

        fun register(
            type: String,
            deserializer: (JsonObject) -> ExcessiveSpawnPermit?,
            serializer: ((ExcessiveSpawnPermit?, JsonObject) -> JsonObject?)? = null
        ): Boolean {
            return types.put(type, PermitAdapter(deserializer, serializer)) == null
        }
    }

    abstract val type: String

    abstract fun isPermitted(cause: SpawnCause): Boolean

    internal data class PermitAdapter(
        val deserializer: (JsonObject) -> ExcessiveSpawnPermit?,
        val serializer: ((ExcessiveSpawnPermit?, JsonObject) -> JsonObject?)?
    )
}

class BucketSpawnPermit(val bucket: String) : ExcessiveSpawnPermit() {
    companion object {
        const val TYPE = "bucket"
    }

    override val type = TYPE

    override fun isPermitted(cause: SpawnCause) = cause.bucket.name == bucket
}

class PlayerSpawnPermit(val name: String) : ExcessiveSpawnPermit() {
    companion object {
        const val TYPE = "player"
    }

    override val type = TYPE

    override fun isPermitted(cause: SpawnCause) = cause.entityUUID?.getPlayer()?.name?.string?.lowercase() == name.lowercase()
}

class LevelSpawnPermit(val level: ResourceLocation) : ExcessiveSpawnPermit() {
    companion object {
        const val TYPE = "level"
    }

    override val type = TYPE

    override fun isPermitted(cause: SpawnCause) = cause.entityWorldId?.location() == level
}

class CompositeSpawnPermit(val permits: List<ExcessiveSpawnPermit>) : ExcessiveSpawnPermit() {
    companion object {
        const val TYPE = "composite"
    }

    override val type = TYPE

    @delegate:Transient
    val groupedPermits by lazy { permits.groupBy { it.type } }

    override fun isPermitted(cause: SpawnCause) = groupedPermits.isNotEmpty() && groupedPermits.all { it.value.any { permit -> permit.isPermitted(cause) } }
}