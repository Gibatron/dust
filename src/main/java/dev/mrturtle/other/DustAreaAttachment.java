package dev.mrturtle.other;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;

public record DustAreaAttachment(HashMap<String, DustArea> areas) {
    public static final Codec<DustAreaAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, DustArea.CODEC).xmap(
                    HashMap::new,
                    HashMap::new
            ).fieldOf("areas").forGetter(DustAreaAttachment::areas)
    ).apply(instance, DustAreaAttachment::new));
}