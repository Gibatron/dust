package dev.mrturtle.item;

import dev.mrturtle.Dust;
import dev.mrturtle.DustItems;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class DustBunnyModelItem extends Item implements PolymerItem {
    private final HashMap<Integer, PolymerModelData> modelData = new HashMap<>();

    public enum ModelPart {
        BLADE,
        EYES
    }

    public DustBunnyModelItem(Settings settings) {
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

    public void registerModel(String modelPath) {
        for (ModelPart part : ModelPart.values()) {
            Identifier id = Dust.id("%s/dust_bunny_%s".formatted(modelPath, part.name().toLowerCase()));
            PolymerModelData model = PolymerResourcePackUtils.requestModel(Items.PAPER, id);
            modelData.put(part.ordinal(), model);
        }
    }

    public static ItemStack getStackForPart(ModelPart part) {
        ItemStack stack = new ItemStack(DustItems.DUST_BUNNY_MODEL);
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(part.ordinal()));
        return stack;
    }
}
