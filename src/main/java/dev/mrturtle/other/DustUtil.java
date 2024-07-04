package dev.mrturtle.other;

import dev.mrturtle.Dust;
import dev.mrturtle.access.WorldChunkAccess;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DustUtil {
    public static void setDustAt(World world, BlockPos pos, float amount) {
        WorldChunk chunk = world.getWorldChunk(pos);
        DustAttachment attachment = chunk.getAttachedOrCreate(Dust.DUST_ATTACHMENT, () -> new DustAttachment(new HashMap<>()));
        float newValue = amount;
        newValue = MathHelper.clamp(newValue, DustElementHolder.MIN_DUST_VALUE, DustElementHolder.MAX_DUST_VALUE);
        attachment.values().put(pos, newValue);
        chunk.setNeedsSaving(true);
        if (((WorldChunkAccess) chunk).dust$getDustElementHolder() != null)
            ((WorldChunkAccess) chunk).dust$getDustElementHolder().updateValues(attachment.values());
        else
            new DustElementHolder(chunk, attachment.values());
    }

    public static void modifyDustAt(World world, BlockPos pos, float amount) {
        WorldChunk chunk = world.getWorldChunk(pos);
        DustAttachment attachment = chunk.getAttachedOrCreate(Dust.DUST_ATTACHMENT, () -> new DustAttachment(new HashMap<>()));
        float newValue = attachment.values().getOrDefault(pos, 0f) + amount;
        newValue = MathHelper.clamp(newValue, DustElementHolder.MIN_DUST_VALUE, DustElementHolder.MAX_DUST_VALUE);
        attachment.values().put(pos, newValue);
        chunk.setNeedsSaving(true);
        if (((WorldChunkAccess) chunk).dust$getDustElementHolder() != null)
            ((WorldChunkAccess) chunk).dust$getDustElementHolder().updateValues(attachment.values());
        else
            new DustElementHolder(chunk, attachment.values());
    }

    public static void removeDustAt(World world, BlockPos pos) {
        WorldChunk chunk = world.getWorldChunk(pos);
        DustAttachment attachment = chunk.getAttachedOrCreate(Dust.DUST_ATTACHMENT, () -> new DustAttachment(new HashMap<>()));
        attachment.values().remove(pos);
        chunk.setNeedsSaving(true);
        if (((WorldChunkAccess) chunk).dust$getDustElementHolder() != null)
            ((WorldChunkAccess) chunk).dust$getDustElementHolder().updateValues(attachment.values());
        else
            new DustElementHolder(chunk, attachment.values());
    }

    public static float getDustAt(World world, BlockPos pos) {
        WorldChunk chunk = world.getWorldChunk(pos);
        if (!chunk.hasAttached(Dust.DUST_ATTACHMENT))
            return 0f;
        DustAttachment attachment = chunk.getAttached(Dust.DUST_ATTACHMENT);
        return attachment.values().getOrDefault(pos, 0f);
    }

    public static boolean isValidDustPlacement(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isFullCube(world, pos))
            return false;
        List<BlockPos> exposedNeighbors = getExposedNeighbors(world, pos);
        if (exposedNeighbors.isEmpty())
            return false;
        for (BlockPos offsetPos : exposedNeighbors) {
            if (world.getLightLevel(LightType.SKY, offsetPos) > DustElementHolder.MAX_SKY_LIGHT)
                return false;
        }
        return true;
    }

    public static List<BlockPos> getExposedNeighbors(World world, BlockPos pos) {
        ArrayList<BlockPos> exposed = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos offsetPos = pos.offset(direction);
            BlockState state = world.getBlockState(offsetPos);
            if (state.isFullCube(world, offsetPos))
                continue;
            if (!state.getFluidState().isEmpty())
                continue;
            exposed.add(offsetPos);
        }
        return exposed;
    }

    public static boolean isInsideDustArea(ServerWorld world, BlockPos pos) {
        if (!world.hasAttached(Dust.DUST_AREA_ATTACHMENT))
            return false;

        DustAreaAttachment attachment = world.getAttachedOrCreate(Dust.DUST_AREA_ATTACHMENT, () -> new DustAreaAttachment(new HashMap<>()));
        for (DustArea area : attachment.areas().values()) {
            if (area.bounds().contains(pos))
                return true;
        }
        return false;
    }
}
