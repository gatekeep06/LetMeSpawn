package com.metacontent

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.position.calculators.SpawnablePositionCalculator.Companion.prioritizedAreaCalculators
import com.cobblemon.mod.common.api.spawning.spawner.Spawner
import com.cobblemon.mod.common.api.spawning.spawner.Spawner.Companion.ENTITY_LIMIT_CHUNK_RANGE
import com.cobblemon.mod.common.api.spawning.spawner.SpawningZoneInput
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
import kotlin.math.max

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

    @JvmStatic
    fun calculateSpawnActions(spawner: Spawner, zoneInput: SpawningZoneInput, maxSpawns: Int?): List<SpawnAction<*>> {
        with(spawner) {
            val maxSpawns = maxSpawns ?: Cobblemon.config.maximumSpawnsPerPass
            influences.removeIf { it.isExpired() }

            val constrainedArea = constrainArea(zoneInput)
                ?: return emptyList()

            val areaBox = AABB.ofSize(
                Vec3(constrainedArea.getCenter().toVec3f()),
                ENTITY_LIMIT_CHUNK_RANGE * 16.0 * 2,
                1000.0,
                ENTITY_LIMIT_CHUNK_RANGE * 16.0 * 2
            )

            if (!constrainedArea.world.isBoxLoaded(areaBox)) {
                return emptyList()
            }

            val numberNearby = constrainedArea.world.getEntitiesOfClass(
                PokemonEntity::class.java,
                areaBox,
                PokemonEntity::countsTowardsSpawnCap
            ).size

            val chunksCovered = ENTITY_LIMIT_CHUNK_RANGE * ENTITY_LIMIT_CHUNK_RANGE
            val maxPokemonPerChunk = max(Cobblemon.config.pokemonPerChunk, zoneInput.cause.spawner.maxPokemonPerChunk)
            val areChunksFilled = numberNearby.toFloat() / chunksCovered >= maxPokemonPerChunk
            val bucket = chooseBucket(zoneInput.cause, influences)
            if (areChunksFilled && !config.permits.any {
                    it?.isPermitted(
                        bucket = bucket,
                        cause = zoneInput.cause,
                        pokemonAmount = numberNearby
                    ) == true
                }) {
                return emptyList()
            }

            val zone = generator.generate(spawner, constrainedArea)
            val spawnablePositions = resolver.resolve(spawner, prioritizedAreaCalculators, zone)
            val influences = influences + zone.unconditionalInfluences

            return selector.select(
                spawner = this,
                bucket = bucket,
                spawnablePositions = spawnablePositions,
                maxSpawns = maxSpawns
            )
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