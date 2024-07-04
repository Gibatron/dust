package dev.mrturtle.other;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockBox;

public record DustArea(BlockBox bounds) {
    public static final Codec<DustArea> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockBox.CODEC.fieldOf("bounds").forGetter(DustArea::bounds)
    ).apply(instance, DustArea::new));
}
