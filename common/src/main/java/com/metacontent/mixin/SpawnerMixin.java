package com.metacontent.mixin;

import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.spawner.Spawner;
import com.cobblemon.mod.common.api.spawning.spawner.SpawningZoneInput;
import com.metacontent.LetMeSpawn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Spawner.class)
public abstract class SpawnerMixin {
    @Inject(method = "calculateSpawnActionsForArea", at = @At("HEAD"), cancellable = true, remap = false)
    protected void inject(SpawningZoneInput zoneInput, Integer maxSpawns, CallbackInfoReturnable<List<SpawnAction<?>>> cir) {
        cir.setReturnValue(LetMeSpawn.calculateSpawnActions((Spawner) this, zoneInput, maxSpawns));
        cir.cancel();
    }
}
