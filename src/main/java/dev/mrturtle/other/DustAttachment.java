package dev.mrturtle.other;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public record DustAttachment(Map<BlockPos, Float> values) {
    // This is uh- not ideal
    public static final Codec<DustAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).xmap((stringMap) -> {
                Map<BlockPos, Float> posMap = new HashMap<>();
                stringMap.forEach((key, value) -> {
                    String[] splitString = key.split(":");
                    int x = Integer.parseInt(splitString[0]);
                    int y = Integer.parseInt(splitString[1]);
                    int z = Integer.parseInt(splitString[2]);
                    BlockPos pos = new BlockPos(x, y, z);
                    posMap.put(pos, value);
                });
                return posMap;
            }, (posMap) -> {
                Map<String, Float> stringMap = new HashMap<>();
                posMap.forEach((key, value) -> {
                    String string = "%s:%s:%s".formatted(key.getX(), key.getY(), key.getZ());
                    stringMap.put(string, value);
                });
                return stringMap;
            }).fieldOf("values").forGetter(DustAttachment::values)
    ).apply(instance, DustAttachment::new));
}
