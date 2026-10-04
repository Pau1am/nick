package com.dongchengqiao.nick;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Client-side lookups for nicknames and teams.
 * <p>
 * Nicknames are read from the tab list entry rather than from a player entity, because
 * {@code ClientLevel#players()} only contains the entities this client is currently tracking -
 * anyone further away is missing from it, and read-only callers would silently get no nickname.
 * The tab list, by contrast, is sent for every online player.
 */
@Environment(EnvType.CLIENT)
public final class NickClientNames {
	private NickClientNames() {
	}

	/** Every online player, or an empty collection when not connected. */
	public static Collection<PlayerInfo> onlinePlayers() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection != null ? connection.getOnlinePlayers() : List.of();
	}

	/** @return the tab list entry for {@code id}, or {@code null} when it is not known. */
	public static PlayerInfo infoOf(UUID id) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection != null ? connection.getPlayerInfo(id) : null;
	}

	/**
	 * The nickname the server published for a player, or {@code null} when they have none.
	 * <p>
	 * A missing tab list display name means no nickname: the server only sets it to advertise one.
	 */
	public static String nicknameOf(PlayerInfo info) {
		if (info == null) {
			return null;
		}
		Component display = info.getTabListDisplayName();
		if (display == null) {
			return null;
		}
		PlayerTeam team = teamOf(info.getProfile().name());
		String prefix = team != null ? team.getPlayerPrefix().getString() : "";
		String suffix = team != null ? team.getPlayerSuffix().getString() : "";
		String plain = NickDisplayText.stripTeamDecoration(display.getString(), prefix, suffix);
		return plain.isEmpty() ? null : plain;
	}

	/** @return the scoreboard team of {@code realName}, or {@code null} if they are in no team. */
	public static PlayerTeam teamOf(String realName) {
		ClientLevel level = Minecraft.getInstance().level;
		return level != null ? level.getScoreboard().getPlayersTeam(realName) : null;
	}

	/** Team colour/prefix/suffix around {@code name}. */
	public static MutableComponent withTeam(PlayerTeam team, Component name) {
		return PlayerTeam.formatNameForTeam(team, name);
	}
}
