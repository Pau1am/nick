package com.dongchengqiao.nick;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Pushes a nickname (or the real name again) to every client, and builds the display names used
 * where vanilla would otherwise render the real profile name.
 */
public final class NickDisplay {
	private NickDisplay() {
	}

	public static void apply(ServerPlayer player, String nick) {
		player.setCustomName(Component.literal(nick));
		player.setCustomNameVisible(true);
		refreshTabList(player);
	}

	public static void reset(ServerPlayer player) {
		player.setCustomName(null);
		player.setCustomNameVisible(false);
		refreshTabList(player);
	}

	/** Re-sends the display name so the tab list entry updates immediately. */
	public static void refreshTabList(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		server.getPlayerList().broadcastAll(
			new ClientboundPlayerInfoUpdatePacket(
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player
			)
		);
	}

	/**
	 * Team colour/prefix/suffix only.
	 * <p>
	 * This mirrors what vanilla produces for a tab list entry, where
	 * {@code ServerPlayer#getTabListDisplayName()} is {@code null} and the client formats the plain
	 * profile name with the team. Adding click/hover decoration here would be a deviation vanilla
	 * never has.
	 */
	public static MutableComponent formatPlain(ServerPlayer player, Component name) {
		return PlayerTeam.formatNameForTeam(player.getTeam(), name);
	}

	/**
	 * Team formatting plus the click/hover/insertion decoration vanilla puts on display names.
	 * <p>
	 * {@code Player#getName()} returns the real profile name and ignores the custom name, so vanilla
	 * never renders a nickname from it and every display path has to be patched explicitly - and has
	 * to add this decoration back, otherwise chat loses click-to-whisper and the hover player card.
	 */
	public static MutableComponent formatForChat(ServerPlayer player, Component name) {
		return formatPlain(player, name).withStyle(style -> style
			.withClickEvent(new ClickEvent.SuggestCommand("/tell " + player.getGameProfile().name() + " "))
			.withHoverEvent(new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(
				player.getType(), player.getUUID(), Component.literal(player.getGameProfile().name()))))
			.withInsertion(player.getGameProfile().name()));
	}
}
