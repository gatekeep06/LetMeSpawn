package com.metacontent.letmespawn.neoforge

import com.metacontent.letmespawn.LetMeSpawn
import net.neoforged.fml.common.Mod
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(LetMeSpawn.ID)
class LetMeSpawnNeoForge {
    init {
        with(MOD_BUS) {
            LetMeSpawn.init()
        }
    }
}