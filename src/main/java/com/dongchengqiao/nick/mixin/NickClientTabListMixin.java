package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickClientConfig;
import com.dongchengqiao.nick.NickClientConfig.DisplayLocation;
import com.dongchengqiao.nick.NickClientConfig.DisplayMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Client-side tab list override. The server already sends the nickname as the tab list name
 * (see {@code NickTabListMixin}), this mixin only applies the per-location display mode.
 */
@Mixin(PlayerInfo.class)
public class NickClientTabListMixin {
	@Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
	private void nick$getTabListDisplayName(CallbackInfoReturnable<Component> cir) {
		PlayerInfo self = (PlayerInfo) (Object) this;
		DisplayMode mode = NickClientConfig.getDisplayMode(DisplayLocation.TAB_LIST);
		if (mode == DisplayMode.DEFAULT) {
			return;
		}

		String originalName = self.getProfile().name();
		Component display;
		switch (mode) {
			case HIDE -> display = Component.literal(originalName);
			case NICK_ONLY -> {
				String nickname = nick$findNickname(self.getProfile().id());
				if (nickname == null) return;
				display = Component.literal(nickname);
			}
			case NICK_AND_ORIGINAL -> {
				String nickname = nick$findNickname(self.getProfile().id());
				if (nickname == null) return;
				display = Component.literal("[" + nickname + "]" + originalName);
			}
			default -> {
				return;
			}
		}

		cir.setReturnValue(PlayerTeam.formatNameForTeam(nick$getTeamForPlayer(originalName), display));
	}

	@Unique
	private static PlayerTeam nick$getTeamForPlayer(String playerName) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		Scoreboard scoreboard = mc.level.getScoreboard();
		return scoreboard.getPlayersTeam(playerName);
	}

	@Unique
	private static String nick$findNickname(UUID uuid) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		for (Player player : mc.level.players()) {
			if (player.getUUID().equals(uuid)) {
				Component customName = player.getCustomName();
				return customName != null ? customName.getString() : null;
			}
		}
		return null;
	}
}
