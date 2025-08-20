package com.metacontent.mixin;

import com.cobblemon.mod.common.api.spawning.SpawnCause;
import com.cobblemon.mod.common.api.spawning.context.SpawningContext;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.spawner.AreaSpawner;
import com.metacontent.LetMeSpawn;
import kotlin.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AreaSpawner.class)
public class AreaSpawnerMixin {
    @Inject(method = "run", at = @At("HEAD"), cancellable = true, remap = false)
    protected void inject(SpawnCause cause, CallbackInfoReturnable<Pair<SpawningContext, SpawnDetail>> cir) {
        cir.setReturnValue(LetMeSpawn.INSTANCE.runSpawn(((AreaSpawner)(Object)this), cause));
        cir.cancel();
    }
}
