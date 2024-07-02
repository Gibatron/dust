package dev.mrturtle;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mrturtle.other.DustAttachment;
import dev.mrturtle.other.DustElementHolder;
import dev.mrturtle.other.DustUtil;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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