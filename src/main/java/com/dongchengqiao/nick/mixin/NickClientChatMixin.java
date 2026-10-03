package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickClientConfig;
import com.dongchengqiao.nick.NickClientConfig.DisplayLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientPacketListener.class)
public class NickClientChatMixin {
	@Unique
	private UUID nick$chatSender;

	@Inject(method = "handlePlayerChat", at = @At("HEAD"))
	private void nick$captureChatSender(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
		this.nick$chatSender = packet.sender();
	}

	@Inject(method = "handlePlayerChat", at = @At("RETURN"))
	private void nick$clearChatSender(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
		this.nick$chatSender = null;
	}

	@ModifyArg(
		method = "handlePlayerChat",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handlePlayerChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/chat/ChatType$Bound;)V"
		),
		index = 2
	)
	private ChatType.Bound nick$modifyChatBound(ChatType.Bound bound) {
		UUID uuid = this.nick$chatSender;
		if (uuid == null) return bound;

		PlayerInfo info = ((ClientPacketListener) (Object) this).getPlayerInfo(uuid);
		if (info == null) return bound;

		String originalName = info.getProfile().name();
		Component displayName;
		switch (NickClientConfig.getDisplayMode(DisplayLocation.CHAT)) {
			case HIDE -> displayName = Component.literal(originalName);
			case NICK_ONLY -> {
				String nickname = nick$findNickname(uuid);
				if (nickname == null) return bound;
				displayName = Component.literal(nickname);
			}
			case NICK_AND_ORIGINAL -> {
				String nickname = nick$findNickname(uuid);
				if (nickname == null) return bound;
				displayName = Component.literal("[" + nickname + "]" + originalName);
			}
			default -> {
				return bound;
			}
		}

		PlayerTeam team = nick$getTeamForPlayer(originalName);
		return new ChatType.Bound(
			bound.chatType(),
			PlayerTeam.formatNameForTeam(team, displayName),
			bound.targetName()
		);
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

	@Unique
	private static PlayerTeam nick$getTeamForPlayer(String playerName) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		Scoreboard scoreboard = mc.level.getScoreboard();
		return scoreboard.getPlayersTeam(playerName);
	}
}
