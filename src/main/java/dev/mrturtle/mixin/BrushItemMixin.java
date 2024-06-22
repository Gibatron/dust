package dev.mrturtle.mixin;

import dev.mrturtle.other.DustUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushItem.class)
public abstract class BrushItemMixin {
    @Shadow protected abstract HitResult getHitResult(PlayerEntity user);

    @Inject(method = "usageTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BrushItem;getMaxUseTime(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)I"))
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (world.isClient())
            return;
        HitResult hitResult = getHitResult((PlayerEntity) user);
        if (!(hitResult instanceof BlockHitResult blockHitResult))
            return;
        if (hitResult.getType() != HitResult.Type.BLOCK)
            return;
        BlockPos blockPos = blockHitResult.getBlockPos();
        DustUtil.modifyDustAt(world, blockPos, -0.05f);
    }
}
