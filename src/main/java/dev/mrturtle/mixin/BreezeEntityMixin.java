package dev.mrturtle.mixin;

import dev.mrturtle.other.DustElementHolder;
import dev.mrturtle.other.DustUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BreezeEntity.class)
public abstract class BreezeEntityMixin extends HostileEntity {
    @Unique
    private int ticksTillBlowDust = 5;

    protected BreezeEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (getWorld().isClient)
            return;
        ticksTillBlowDust -= 1;
        if (ticksTillBlowDust <= 0) {
            ticksTillBlowDust = 5;
            Iterable<BlockPos> positions = BlockPos.iterate(getBlockPos().add(-1, -1, -1), getBlockPos().add(1, 1, 1));
            for (BlockPos blockPos : positions) {
                float dustValue = DustUtil.getDustAt(getWorld(), blockPos.toImmutable());
                if (dustValue <= DustElementHolder.MIN_DUST_VALUE)
                    continue;
                float amount = random.nextFloat() * 0.2f;
                DustUtil.modifyDustAt(getWorld(), blockPos.toImmutable(), -amount);
                if (dustValue >= DustElementHolder.MIN_VISIBLE_DUST_VALUE) {
                    Vec3d center = blockPos.toCenterPos();
                    ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.SPIT, center.getX(), center.getY(), center.getZ(), 5, 0.25, 0.25, 0.25, 0.12);
                }
            }
        }
    }
}
