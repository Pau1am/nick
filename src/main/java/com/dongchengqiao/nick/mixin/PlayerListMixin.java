package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickDisplay;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Shadow
	@Final
	private List<ServerPlayer> players;

	/**
	 * The sender of the message currently being broadcast.
	 * <p>
	 * A plain field is safe here: chat is delivered through {@code FutureChain(MinecraftServer)},
	 * which runs each queued task on the server thread (verified against the 26.3 sources -
	 * {@code FutureChain#append} uses {@code thenAcceptAsync(task, executor)} with the server as
	 * executor), so the write below and the read in the injection always happen on one thread and
	 * cannot interleave. The write also always happens first, because the injection only runs
	 * inside the method body the write sits at the head of.
	 */
	@Unique
	private static ServerPlayer nick$currentSender;

	@Inject(method = "getPlayerByName", at = @At("TAIL"), cancellable = true)
	private void nick$getPlayerByName(String name, CallbackInfoReturnable<ServerPlayer> cir) {
		if (cir.getReturnValue() != null) {
			return;
		}
		// Real names always win; only fall back to nicknames. Case-insensitive, like vanilla.
		for (ServerPlayer player : this.players) {
			Component customName = player.getCustomName();
			if (customName != null && customName.getString().equalsIgnoreCase(name)) {
				cir.setReturnValue(player);
				return;
			}
		}
	}

	@Inject(method = "getPlayerNamesArray", at = @At("HEAD"), cancellable = true)
	private void nick$getPlayerNamesArray(CallbackInfoReturnable<String[]> cir) {
		String[] names = new String[this.players.size()];
		for (int i = 0; i < this.players.size(); i++) {
			ServerPlayer player = this.players.get(i);
			Component customName = player.getCustomName();
			names[i] = customName != null ? customName.getString() : player.getGameProfile().name();
		}
		cir.setReturnValue(names);
	}

	@Inject(
		method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
		at = @At("HEAD")
	)
	private void nick$captureSender(PlayerChatMessage message, Predicate<ServerPlayer> isFiltered,
									ServerPlayer sender, ChatType.Bound bound, CallbackInfo ci) {
		nick$currentSender = sender;
	}

	/**
	 * Vanilla builds the chat bound from {@code Player#getDisplayName()}, which resolves
	 * {@code Player#getName()} - the real name - so it ignores the custom name.
	 */
	@ModifyArg(
		method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V"
		),
		index = 2
	)
	private ChatType.Bound nick$modifyBound(ChatType.Bound bound) {
		ServerPlayer sender = nick$currentSender;
		if (sender == null) {
			return bound;
		}
		Component customName = sender.getCustomName();
		if (customName == null) {
			return bound;
		}
		return new ChatType.Bound(bound.chatType(), NickDisplay.formatForChat(sender, customName), bound.targetName());
	}
}
