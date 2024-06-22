package dev.mrturtle.other;

import dev.mrturtle.DustItems;
import dev.mrturtle.access.WorldChunkAccess;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DustElementHolder extends ElementHolder {
    public static final float MIN_DUST_VALUE = 0.0f;
    public static final float MAX_DUST_VALUE = 3.5f;
    public static final float MIN_VISIBLE_DUST_VALUE = 1.0f;

    public static final float DUST_ACCUMULATION_RATE = 0.05f;
    public static final int MAX_SKY_LIGHT = 8;

    private final Map<BlockPos, ItemDisplayElement> positionsToElements = new HashMap<>();
    private final WorldChunk chunk;

    private Map<BlockPos, Float> values = new HashMap<>();

    public DustElementHolder(WorldChunk chunk, Map<BlockPos, Float> values) {
        this.chunk = chunk;
        updateValues(values);
        new ChunkAttachment(this, chunk, chunk.getPos().getStartPos().toCenterPos(), false);
        ((WorldChunkAccess) chunk).dust$setDustElementHolder(this);
    }

    public void updateValues(Map<BlockPos, Float> newValues) {
        ArrayList<BlockPos> dustToRemove = new ArrayList<>();
        ArrayList<BlockPos> dustToAdd = new ArrayList<>();
        ArrayList<BlockPos> dustToUpdate = new ArrayList<>();
        for (BlockPos key : values.keySet()) {
            if (!newValues.containsKey(key))
                dustToRemove.add(key);
            else if (!values.get(key).equals(newValues.get(key)))
                dustToUpdate.add(key);
        }
        for (BlockPos key : newValues.keySet()) {
            if (!values.containsKey(key))
                dustToAdd.add(key);
        }
        for (BlockPos key : dustToRemove) {
            removeDust(key);
        }
        for (BlockPos key : dustToAdd) {
            addDust(key, newValues.get(key));
        }
        for (BlockPos key : dustToUpdate) {
            float newValue = newValues.get(key);
            if (newValue < MIN_VISIBLE_DUST_VALUE) {
                removeDust(key);
                continue;
            }
            if (!positionsToElements.containsKey(key)) {
                addDust(key, newValue);
                continue;
            }
            ItemDisplayElement overlay = positionsToElements.get(key);
            ItemStack stack = overlay.getItem();
            int index = dustValueToIndex(newValue);
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(index));
            overlay.setItem(ItemStack.EMPTY);
            overlay.tick();
            overlay.setItem(stack);
            overlay.tick();
        }
        values = new HashMap<>(newValues);
    }

    private void addDust(BlockPos pos, float value) {
        if (value < MIN_VISIBLE_DUST_VALUE)
            return;
        ItemDisplayElement overlay = new ItemDisplayElement();
        ItemStack stack = DustItems.DUST_OVERLAY.getDefaultStack();
        int index = dustValueToIndex(value);
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(index));
        overlay.setItem(stack);
        overlay.setScale(new Vector3f(1.002f));
        overlay.setBrightness(Brightness.FULL);
        Vec3d offset = pos.toCenterPos().subtract(chunk.getPos().getStartPos().toCenterPos());
        overlay.setOffset(offset);
        addElement(overlay);
        positionsToElements.put(pos, overlay);
    }

    private void removeDust(BlockPos key) {
        removeElement(positionsToElements.get(key));
        positionsToElements.remove(key);
    }

    private int dustValueToIndex(float dustValue) {
        if (dustValue < 2)
            return 0;
        else if (dustValue < 3)
            return 1;
        else
            return 2;
    }
}
