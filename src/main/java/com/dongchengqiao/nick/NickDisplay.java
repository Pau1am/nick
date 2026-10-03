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
 * Pushes a nickname (or the real name again) to every client, and builds display names that
 * keep vanilla's team colour/prefix/suffix and click/hover decoration.
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
	 * Team formatting plus the click/hover behaviour vanilla puts on player display names.
	 * <p>
	 * {@code Player#getName()} ignores the custom name, so vanilla never renders a nickname from
	 * it and every display path has to be patched explicitly - and has to add this decoration back,
	 * otherwise chat loses click-to-whisper and the hover player card.
	 */
	public static MutableComponent decorate(ServerPlayer player, Component name) {
		MutableComponent formatted = PlayerTeam.formatNameForTeam(player.getTeam(), name);
		String realName = player.getGameProfile().name();
		return formatted.withStyle(style -> style
			.withClickEvent(new ClickEvent.SuggestCommand("/tell " + realName + " "))
			.withHoverEvent(new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(
				player.getType(), player.getUUID(), Component.literal(realName))))
			.withInsertion(realName));
	}
}
