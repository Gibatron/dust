package dev.mrturtle;

import dev.mrturtle.item.DustBunnyModelItem;
import dev.mrturtle.item.DustOverlayItem;
import dev.mrturtle.item.SimpleModeledPolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.core.api.item.PolymerSpawnEggItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public class DustItems {
    public static final Item DUST_OVERLAY = register(new DustOverlayItem(new Item.Settings()), "dust_overlay", "block");
    public static final Item DUST_BUNNY_MODEL = register(new DustBunnyModelItem(new Item.Settings()), "dust_bunny_model", "entity");
    public static final Item DUST_BUNNY_HIDE = register(new SimpleModeledPolymerItem(new Item.Settings(), Items.PAPER), "dust_bunny_hide", "item");
    public static final Item DUST_BUNNY_SPAWN_EGG = register(new PolymerSpawnEggItem(DustEntities.DUST_BUNNY, Items.SILVERFISH_SPAWN_EGG, new Item.Settings()), "dust_bunny_spawn_egg", "item");

    public static void initialize() {
        PolymerItemGroupUtils.registerPolymerItemGroup(Dust.id("group"), ItemGroup.create(ItemGroup.Row.BOTTOM, -1)
                .icon(Items.BRUSH::getDefaultStack)
                .displayName(Text.translatable("itemgroup.dust"))
                .entries(((context, entries) -> {
                    entries.add(DUST_BUNNY_SPAWN_EGG);
                    entries.add(DUST_BUNNY_HIDE);
                    entries.add(Items.BRUSH);
                })).build());
    }

    public static <T extends Item> T register(T item, String id, String modelPath) {
        if (item instanceof SimpleModeledPolymerItem)
            ((SimpleModeledPolymerItem) item).registerModel(id, modelPath);
        // This isn't the best, but we're going to deal with it
        if (item instanceof DustOverlayItem)
            ((DustOverlayItem) item).registerModel(id, modelPath);
        if (item instanceof DustBunnyModelItem)
            ((DustBunnyModelItem) item).registerModel(modelPath);
        return Registry.register(Registries.ITEM, Dust.id(id), item);
    }
}
