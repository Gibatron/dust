package dev.mrturtle.mixin;

import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract boolean isOf(Item item);

    @Inject(method = "canPlaceOn", at = @At("HEAD"), cancellable = true)
    public void brushEmergencyMixin(CachedBlockPosition pos, CallbackInfoReturnable<Boolean> cir) {
        if (isOf(Items.BRUSH))
            cir.setReturnValue(true);
    }
}
