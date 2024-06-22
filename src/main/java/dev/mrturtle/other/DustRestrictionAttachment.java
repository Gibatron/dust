package dev.mrturtle.other;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockBox;

public record DustRestrictionAttachment(BlockBox bounds) {
    public static final Codec<DustRestrictionAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockBox.CODEC.fieldOf("bounds").forGetter(DustRestrictionAttachment::bounds)
    ).apply(instance, DustRestrictionAttachment::new));
}
