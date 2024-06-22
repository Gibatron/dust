package dev.mrturtle.mixin;

import dev.mrturtle.other.DustElementHolder;
import dev.mrturtle.other.DustUtil;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld))
            return;
        if (state.isOf(newState.getBlock()))
            return;
        float dustValue = DustUtil.getDustAt(world, pos);
        DustUtil.removeDustAt(world, pos);
        if (dustValue < DustElementHolder.MIN_VISIBLE_DUST_VALUE)
            return;
        // Effects
        Vec3d center = pos.toCenterPos();
        serverWorld.spawnParticles(ParticleTypes.SPIT, center.getX(), center.getY(), center.getZ(), (int) (dustValue * 10), 0.25, 0.25, 0.25, 0.12);
        // Pitch is random between 0.8 and 1.2
        float pitch = 0.8f + (world.random.nextFloat() * 0.4f);
        serverWorld.playSound(null, pos, SoundEvents.ENTITY_SHULKER_BULLET_HIT, SoundCategory.BLOCKS, 0.6f, pitch);
        // Spread dust
        ArrayList<BlockPos> validPositions = new ArrayList<>();
        Iterable<BlockPos> positions = BlockPos.iterateRandomly(world.random, 3 * 3 * 3, pos, 1);
        for (BlockPos blockPos : positions) {
            if (!DustUtil.isValidDustPlacement(world, blockPos.toImmutable()))
                continue;
            if (DustUtil.getDustAt(world, blockPos.toImmutable()) >= DustElementHolder.MAX_DUST_VALUE)
                continue;
            validPositions.add(blockPos.toImmutable());
        }
        for (BlockPos blockPos : validPositions) {
            DustUtil.modifyDustAt(world, blockPos.toImmutable(), dustValue / validPositions.size());
        }
    }
}
