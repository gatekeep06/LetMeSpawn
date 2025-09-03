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

    abstract val max: Int?

    abstract fun isPermitted(cause: SpawnCause, pokemonAmount: Int): Boolean

    internal data class PermitAdapter(
        val deserializer: (JsonObject) -> ExcessiveSpawnPermit?,
        val serializer: ((ExcessiveSpawnPermit?, JsonObject) -> JsonObject?)?
    )
}

class BucketSpawnPermit(val bucket: String, override val max: Int?) : ExcessiveSpawnPermit() {
    companion object {
        const val TYPE = "bucket"
    }

    override val type = TYPE

    override fun isPermitted(cause: SpawnCause, pokemonAmount: Int) =
        cause.bucket.name == bucket && (max == null || max < 0 || pokemonAmount < max)
}

class PlayerSpawnPermit(val name: String, override val max: Int?) : ExcessiveSpawnPermit() {
    companion object {
        const val TYPE = "player"
    }

    override val type = TYPE

    override fun isPermitted(cause: SpawnCause, pokemonAmount: Int) =
        cause.entityUUID?.getPlayer()?.name?.string?.lowercase() == name.lowercase() && (max == null || max < 0 || pokemonAmount < max)
}

class LevelSpawnPermit(val level: ResourceLocation, override val max: Int?) : ExcessiveSpawnPermit() {
    companion object {
        const val TYPE = "level"
    }

    override val type = TYPE

    override fun isPermitted(cause: SpawnCause, pokemonAmount: Int) =
        cause.entityWorldId?.location() == level && (max == null || max < 0 || pokemonAmount < max)
}

class CompositeSpawnPermit(val permits: List<ExcessiveSpawnPermit>) : ExcessiveSpawnPermit() {
    companion object {
        const val TYPE = "composite"
    }

    override val type = TYPE

    override val max = null

    @delegate:Transient
    val groupedPermits by lazy { permits.groupBy { it.type } }

    override fun isPermitted(
        cause: SpawnCause,
        pokemonAmount: Int
    ) = groupedPermits.isNotEmpty() && groupedPermits.all {
        it.value.any { permit ->
            permit.isPermitted(
                cause,
                pokemonAmount
            )
        }
    }
}