package dev.mrturtle.mixin;

import dev.mrturtle.other.DustElementHolder;
import dev.mrturtle.access.WorldChunkAccess;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldChunk.class)
public class WorldChunkMixin implements WorldChunkAccess {
    @Unique
    DustElementHolder dustElementHolder;

    public void dust$setDustElementHolder(DustElementHolder elementHolder) {
        this.dustElementHolder = elementHolder;
    }

    public DustElementHolder dust$getDustElementHolder() {
        return dustElementHolder;
    }
}
