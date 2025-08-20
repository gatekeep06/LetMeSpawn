package com.metacontent.fabric

import com.metacontent.LetMeSpawn
import net.fabricmc.api.ModInitializer

class LetMeSpawnFabric : ModInitializer {
    override fun onInitialize() {
        LetMeSpawn.init()
    }
}