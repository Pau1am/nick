package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickClientConfig;
import com.dongchengqiao.nick.NickClientConfig.DisplayLocation;
import com.dongchengqiao.nick.NickClientConfig.DisplayMode;
import com.dongchengqiao.nick.NickClientNames;
import com.dongchengqiao.nick.NickDisplayText;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Client-side chat override, applying the per-location display mode.
 * <p>
 * The server already replaces the sender name with the nickname (see {@code PlayerListMixin}),
 * so {@code NICK_ONLY} needs no work here. The other modes rebuild the name and have to carry the
 * server's style over, otherwise chat loses click-to-whisper and the hover player card.
 */
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
		UUID senderId = this.nick$chatSender;
		DisplayMode mode = NickClientConfig.getDisplayMode(DisplayLocation.CHAT);
		if (senderId == null || mode == DisplayMode.NICK_ONLY) {
			return bound;
		}

		PlayerInfo info = NickClientNames.infoOf(senderId);
		String nickname = NickClientNames.nicknameOf(info);
		if (info == null || nickname == null) {
			// No nickname in play, so the server left the real name alone and vanilla is correct.
			return bound;
		}

		String realName = info.getProfile().name();
		Component display = NickDisplayText.resolve(mode, realName, nickname)
			.copy()
			.withStyle(bound.name().getStyle());
		return new ChatType.Bound(
			bound.chatType(),
			NickClientNames.withTeam(NickClientNames.teamOf(realName), display),
			bound.targetName()
		);
	}
}
