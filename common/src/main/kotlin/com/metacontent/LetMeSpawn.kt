package com.metacontent

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.spawning.SpawnCause
import com.cobblemon.mod.common.api.spawning.context.SpawningContext
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail
import com.cobblemon.mod.common.api.spawning.spawner.AreaSpawner
import com.cobblemon.mod.common.api.spawning.spawner.AreaSpawner.Companion.CHUNK_REACH
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.platform.events.PlatformEvents
import com.cobblemon.mod.common.util.asResource
import com.cobblemon.mod.common.util.isBoxLoaded
import com.cobblemon.mod.common.util.toVec3f
import com.metacontent.config.*
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object LetMeSpawn {
    const val ID = "let_me_spawn"
    val LOGGER: Logger = LoggerFactory.getLogger(ID)

    lateinit var config: PermitConfig

    fun init() {
        registerPermitTypes()

        PlatformEvents.SERVER_STARTING.subscribe {
            config = PermitConfig.load()
        }
    }

    fun runSpawn(areaSpawner: AreaSpawner, cause: SpawnCause): Pair<SpawningContext, SpawnDetail>? {
        with(areaSpawner) {
            val area = getArea(cause)
            val constrainedArea = if (area != null) constrainArea(area) else null
            if (constrainedArea != null) {

                val areaBox = AABB.ofSize(
                    Vec3(
                        constrainedArea.getCenter().toVec3f()
                    ), CHUNK_REACH * 16.0 * 2, 1000.0, CHUNK_REACH * 16.0 * 2
                )
                if (!constrainedArea.world.isBoxLoaded(areaBox)) {
                    return null
                }

                val numberNearby = constrainedArea.world.getEntitiesOfClass(
                    PokemonEntity::class.java,
                    areaBox,
                    PokemonEntity::countsTowardsSpawnCap
                ).size

                val chunksCovered = CHUNK_REACH * CHUNK_REACH
                val areChunksFilled = numberNearby.toFloat() / chunksCovered >= Cobblemon.config.pokemonPerChunk

                if (areChunksFilled && !config.permits.any { it?.isPermitted(cause, numberNearby) == true }) {
                    return null
                }

                val slice = prospector.prospect(this, constrainedArea)
                val contexts = resolver.resolve(this, contextCalculators, slice)
                return getSpawningSelector().select(this, contexts)?.also {
                    if (areChunksFilled && config.enableSpawnMessages) {
                        LOGGER.info("Chunks are filled, but ${it.second.id} spawned anyway")
                    }
                }
            }

            return null
        }
    }

    fun registerPermitTypes() {
        ExcessiveSpawnPermit.register(
            BucketSpawnPermit.TYPE,
            { json -> json.get("bucket")?.asString?.let { BucketSpawnPermit(it, json.get("max")?.asInt) } }
        )
        ExcessiveSpawnPermit.register(
            PlayerSpawnPermit.TYPE,
            { json -> json.get("name")?.asString?.let { PlayerSpawnPermit(it, json.get("max")?.asInt) } }
        )
        ExcessiveSpawnPermit.register(
            LevelSpawnPermit.TYPE,
            { json -> json.get("level")?.asString?.let { LevelSpawnPermit(it.asResource(), json.get("max")?.asInt) } },
            { permit, json ->
                (permit as? LevelSpawnPermit)?.level?.toString()?.let {
                    json.addProperty("level", it)
                    json
                }
            }
        )
        ExcessiveSpawnPermit.register(
            CompositeSpawnPermit.TYPE,
            { json ->
                json.get("permits")?.asJsonArray?.mapNotNull {
                    SpawnPermitAdapter.deserialize(it.asJsonObject, null, null)
                }?.let { CompositeSpawnPermit(it) }
            }
        )
    }
}