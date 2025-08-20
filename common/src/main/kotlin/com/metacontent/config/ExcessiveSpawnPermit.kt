package com.metacontent.config

import com.cobblemon.mod.common.api.spawning.SpawnCause
import com.cobblemon.mod.common.util.getPlayer
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation

abstract class ExcessiveSpawnPermit() {
    companion object {
        internal val types = mutableMapOf<String, (JsonObject) -> ExcessiveSpawnPermit?>()

        fun register(type: String, parser: (JsonObject) -> ExcessiveSpawnPermit?): Boolean {
            return types.put(type, parser) == null
        }
    }

    abstract val type: String

    abstract fun isPermitted(cause: SpawnCause): Boolean
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

    override fun isPermitted(cause: SpawnCause) = cause.entityUUID?.getPlayer()?.name?.string == name
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