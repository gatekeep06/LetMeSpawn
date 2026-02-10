package com.metacontent.letmespawn.fabric

import com.metacontent.letmespawn.LetMeSpawn
import net.fabricmc.api.ModInitializer

class LetMeSpawnFabric : ModInitializer {
    override fun onInitialize() {
        LetMeSpawn.init()
    }
}