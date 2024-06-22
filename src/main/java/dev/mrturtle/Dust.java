package dev.mrturtle;

import com.mojang.brigadier.arguments.FloatArgumentType;
import dev.mrturtle.other.DustAttachment;
import dev.mrturtle.other.DustElementHolder;
import dev.mrturtle.other.DustRestrictionAttachment;
import dev.mrturtle.other.DustUtil;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
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

	public static final AttachmentType<DustRestrictionAttachment> DUST_RESTRICTION_ATTACHMENT = AttachmentRegistry.createPersistent(
			id("dust_restriction"),
			DustRestrictionAttachment.CODEC
	);

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
												BlockPos fromPos = BlockPosArgumentType.getLoadedBlockPos(context, "from");
												BlockPos toPos = BlockPosArgumentType.getLoadedBlockPos(context, "to");
												float value = FloatArgumentType.getFloat(context, "value");
												Iterable<BlockPos> positions = BlockPos.iterate(fromPos, toPos);
												for (BlockPos blockPos : positions) {
													if (value > 0)
														DustUtil.setDustAt(context.getSource().getWorld(), blockPos.toImmutable(), value);
													else
														DustUtil.removeDustAt(context.getSource().getWorld(), blockPos.toImmutable());
												}
												return 1;
											})
									)
							)
					)
			).then(
					literal("restrict")
							.then(literal("set").then(argument("from", BlockPosArgumentType.blockPos())
									.then(argument("to", BlockPosArgumentType.blockPos()).executes(context -> {
										ServerWorld world = context.getSource().getServer().getOverworld();
										BlockPos fromPos = BlockPosArgumentType.getLoadedBlockPos(context, "from");
										BlockPos toPos = BlockPosArgumentType.getLoadedBlockPos(context, "to");
										BlockBox box = BlockBox.create(fromPos, toPos);
										world.setAttached(DUST_RESTRICTION_ATTACHMENT, new DustRestrictionAttachment(box));
										return 1;
									}))
							))
							.then(literal("remove").executes(context -> {
								ServerWorld world = context.getSource().getServer().getOverworld();
								world.removeAttached(DUST_RESTRICTION_ATTACHMENT);
								return 1;
							}))

			));
		});
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}