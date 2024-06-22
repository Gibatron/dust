package dev.mrturtle.mixin;

import dev.mrturtle.other.DustElementHolder;
import dev.mrturtle.other.DustUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.BreezeWindChargeEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WindChargeEntity.class, BreezeWindChargeEntity.class})
public abstract class AbstractWindChargeEntityMixin extends ExplosiveProjectileEntity {

    protected AbstractWindChargeEntityMixin(EntityType<? extends ExplosiveProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "createExplosion", at = @At("HEAD"))
    public void createExplosion(Vec3d pos, CallbackInfo ci) {
        if (getWorld().isClient)
            return;
        Iterable<BlockPos> positions = BlockPos.iterate(getBlockPos().add(-1, -1, -1), getBlockPos().add(1, 1, 1));
        for (BlockPos blockPos : positions) {
            if (DustUtil.getDustAt(getWorld(), blockPos.toImmutable()) <= DustElementHolder.MIN_DUST_VALUE)
                continue;
            float amount = random.nextFloat() + 1f;
            DustUtil.modifyDustAt(getWorld(), blockPos.toImmutable(), -amount);
            Vec3d center = blockPos.toCenterPos();
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.SPIT, center.getX(), center.getY(), center.getZ(), 5, 0.25, 0.25, 0.25, 0.12);
        }
    }
}
