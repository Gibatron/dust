package dev.mrturtle;

import dev.mrturtle.entity.DustBunnyEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class DustEntities {
    public static final EntityType<DustBunnyEntity> DUST_BUNNY = register(
            EntityType.Builder.create(DustBunnyEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.25f, 0.25f)
                    .disableSaving()
                    .build(), "dust_bunny");

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(DUST_BUNNY, DustBunnyEntity.createDustBunnyAttributes());
    }

    public static <T extends EntityType<?>> T register(T entityType, String id) {
        Identifier entityTypeId = Dust.id(id);
        PolymerEntityUtils.registerType(entityType);
        return Registry.register(Registries.ENTITY_TYPE, entityTypeId, entityType);
    }
}
