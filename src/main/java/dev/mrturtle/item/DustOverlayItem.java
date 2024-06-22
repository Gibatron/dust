package dev.mrturtle.item;

import dev.mrturtle.Dust;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class DustOverlayItem extends Item implements PolymerItem {
    private final HashMap<Integer, PolymerModelData> modelData = new HashMap<>();

    public DustOverlayItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.PAPER;
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        int index = itemStack.getOrDefault(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelDataComponent.DEFAULT).value();
        return modelData.get(index).value();
    }

    public void registerModel(String id, String modelPath) {
        for (int i = 0; i < 3; i++) {
            modelData.put(i, PolymerResourcePackUtils.requestModel(Items.PAPER, Dust.id("%s/%s_%s".formatted(modelPath, id, i))));
        }
    }
}
