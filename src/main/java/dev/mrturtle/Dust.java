package dev.mrturtle;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mrturtle.other.*;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Dust implements ModInitializer {
	public static final String MOD_ID = "dust";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final long TIME_FOR_AFK = 1000 * 60;

	public static final AttachmentType<DustAttachment> DUST_ATTACHMENT = AttachmentRegistry.createPersistent(
			id("dust"),
			DustAttachment.CODEC
	);

	public static final AttachmentType<DustAreaAttachment> DUST_AREA_ATTACHMENT = AttachmentRegistry.createPersistent(
			id("dust_areas"),
			DustAreaAttachment.CODEC
	);

	public static final GameRules.Key<GameRules.BooleanRule> DO_DUST_ACCUMULATION =
			GameRuleRegistry.register("doDustAccumulation", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

	@Override
	public void onInitialize() {
		PolymerResourcePackUtils.addModAssets(MOD_ID);
		PolymerResourcePackUtils.markAsRequired();
		DustItems.initialize();
		DustEntities.initialize();
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
			if (!chunk.hasAttached(DUST_ATTACHMENT))
				return;
			DustAttachment attachment = chunk.getAttached(DUST_ATTACHMENT);
			new DustElementHolder(chunk, attachment.values());
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("dust").requires(source -> source.hasPermissionLevel(2)).then(
					literal("set").then(argument("from", BlockPosArgumentType.blockPos())
							.then(argument("to", BlockPosArgumentType.blockPos())
									.then(argument("value", FloatArgumentType.floatArg(DustElementHolder.MIN_DUST_VALUE, DustElementHolder.MAX_DUST_VALUE))
											.executes(context -> {
												setAreaDustCommand(context, false);
												return 1;
											})
											.then(literal("force").executes(context -> {
												setAreaDustCommand(context, true);
												return 1;
											}))
									)
							)
					)
			).then(literal("area")
					.then(literal("add").then(argument("from", BlockPosArgumentType.blockPos())
							.then(argument("to", BlockPosArgumentType.blockPos())
									.then(argument("id", StringArgumentType.word())
											.executes(context -> {
												BlockPos fromPos = BlockPosArgumentType.getLoadedBlockPos(context, "from");
												BlockPos toPos = BlockPosArgumentType.getLoadedBlockPos(context, "to");
												BlockBox bounds = BlockBox.create(fromPos, toPos);

												String areaId = StringArgumentType.getString(context, "id");

												ServerWorld world = context.getSource().getWorld();
												DustAreaAttachment attachment = world.getAttachedOrCreate(DUST_AREA_ATTACHMENT, () -> new DustAreaAttachment(new HashMap<>()));

												attachment.areas().put(areaId, new DustArea(bounds));

												context.getSource().sendFeedback(() -> Text.literal("Created area with id \"%s\"".formatted(areaId)), false);
												return 1;
											})
									)
							)
					))
					.then(literal("remove")
							.then(argument("id", StringArgumentType.word())
									.executes(context -> {
										String areaId = StringArgumentType.getString(context, "id");

										ServerWorld world = context.getSource().getWorld();
										DustAreaAttachment attachment = world.getAttachedOrCreate(DUST_AREA_ATTACHMENT, () -> new DustAreaAttachment(new HashMap<>()));

										DustArea area = attachment.areas().remove(areaId);
										if (area != null)
											context.getSource().sendFeedback(() -> Text.literal("Removed area with id \"%s\"".formatted(areaId)), false);
										else
											context.getSource().sendFeedback(() -> Text.literal("Found no area with id \"%s\"".formatted(areaId)), false);
										return 1;
									})
							)
					)
					.then(literal("list")
							.executes(context -> {
								ServerWorld world = context.getSource().getWorld();
								if (!world.hasAttached(DUST_AREA_ATTACHMENT)) {
									context.getSource().sendFeedback(() -> Text.literal("There are no areas to list"), false);
									return 1;
								}

								DustAreaAttachment attachment = world.getAttachedOrCreate(DUST_AREA_ATTACHMENT, () -> new DustAreaAttachment(new HashMap<>()));

								context.getSource().sendFeedback(() -> Text.literal("Showing %s areas".formatted(attachment.areas().size())), false);

								for (Map.Entry<String, DustArea> entry : attachment.areas().entrySet()) {
									BlockBox bounds = entry.getValue().bounds();
									String minCorner = "(%s, %s, %s)".formatted(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
									String maxCorner = "(%s, %s, %s)".formatted(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
									context.getSource().sendFeedback(() -> Text.literal("\"%s\" from %s to %s".formatted(entry.getKey(), minCorner, maxCorner)), false);
								}
								return 1;
							})
					)
			));
		});
	}

	public static void setAreaDustCommand(CommandContext<ServerCommandSource> context, boolean forceInvalid) throws CommandSyntaxException {
		BlockPos fromPos = BlockPosArgumentType.getLoadedBlockPos(context, "from");
		BlockPos toPos = BlockPosArgumentType.getLoadedBlockPos(context, "to");
		float value = FloatArgumentType.getFloat(context, "value");
		Iterable<BlockPos> positions = BlockPos.iterate(fromPos, toPos);
		for (BlockPos blockPos : positions) {
			if (value > 0) {
				if (DustUtil.isValidDustPlacement(context.getSource().getWorld(), blockPos.toImmutable()) || forceInvalid)
					DustUtil.setDustAt(context.getSource().getWorld(), blockPos.toImmutable(), value);
			} else {
				DustUtil.removeDustAt(context.getSource().getWorld(), blockPos.toImmutable());
			}
		}
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}