package dev.mrturtle.mixin;

import com.mojang.authlib.GameProfile;
import dev.mrturtle.Dust;
import dev.mrturtle.other.DustElementHolder;
import dev.mrturtle.DustEntities;
import dev.mrturtle.entity.DustBunnyEntity;
import dev.mrturtle.other.DustUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Unique
    private static final float DUST_BUNNY_SPAWN_DELAY = 1200;
    @Unique
    private static final int DUST_BUNNY_SPAWN_COUNT = 2;
    @Unique
    private float ticksSinceLastDustBunnySpawn = DUST_BUNNY_SPAWN_DELAY;

    @Shadow public abstract ServerWorld getServerWorld();

    @Shadow public abstract long getLastActionTime();

    @Shadow @Final public ServerPlayerInteractionManager interactionManager;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void accumulateDust(CallbackInfo ci) {
        // Only accumulate dust near players in survival or adventure
        if (interactionManager.getGameMode() != GameMode.SURVIVAL && interactionManager.getGameMode() != GameMode.ADVENTURE)
            return;
        ticksSinceLastDustBunnySpawn = Math.max(ticksSinceLastDustBunnySpawn - 1, 0);
        boolean isAfk = (Util.getMeasuringTimeMs() - getLastActionTime()) > Dust.TIME_FOR_AFK;
        ServerWorld world = getServerWorld();

        int range = world.random.nextBetween(4, 6);
        Iterable<BlockPos> positions = BlockPos.iterateRandomly(world.random, 2, getBlockPos(), range);
        for (BlockPos blockPos : positions) {
            blockPos = blockPos.toImmutable();
            if (!DustUtil.isValidDustPlacement(world, blockPos))
                continue;

            // Dust cannot accumulate in a world with doDustAccumulation off, unless the block is inside a dust area
            if (!world.getGameRules().getBoolean(Dust.DO_DUST_ACCUMULATION) && !DustUtil.isInsideDustArea(world, blockPos))
                return;

            float dustAmount = DustUtil.getDustAt(world, blockPos);
            if (dustAmount >= DustElementHolder.MAX_DUST_VALUE) {
                if (ticksSinceLastDustBunnySpawn <= 0 && !isAfk) {
                    ticksSinceLastDustBunnySpawn = DUST_BUNNY_SPAWN_DELAY;
                    DustUtil.modifyDustAt(world, blockPos, -0.5f);
                    for (int i = 0; i < DUST_BUNNY_SPAWN_COUNT; i++) {
                        Box box = new Box(blockPos.toCenterPos().add(-5, -5, -5), blockPos.toCenterPos().add(5, 5, 5));
                        int dustBunnyCount = world.getEntitiesByClass(DustBunnyEntity.class, box, (bunny) -> true).size();
                        if (dustBunnyCount >= 2)
                            break;

                        DustBunnyEntity dustBunny = new DustBunnyEntity(DustEntities.DUST_BUNNY, world);
                        Vec3d pos = DustUtil.getExposedNeighbors(world, blockPos).get(0).toCenterPos();
                        dustBunny.setPos(pos.getX(), pos.getY(), pos.getZ());
                        world.spawnEntity(dustBunny);
                    }
                }
            } else {
                DustUtil.modifyDustAt(world, blockPos, DustElementHolder.DUST_ACCUMULATION_RATE);
            }
        }
    }
}
