package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickClientConfig;
import com.dongchengqiao.nick.NickClientConfig.DisplayMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Command suggestions offer the nickname instead of the real name, because that is what players
 * see everywhere else. Honours the "hide nicknames" display mode.
 */
@Mixin(ClientSuggestionProvider.class)
public class NickClientSuggestionsMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "getOnlinePlayerNames", at = @At("RETURN"), cancellable = true)
	private void nick$getOnlinePlayerNames(CallbackInfoReturnable<Collection<String>> cir) {
		Collection<String> originalNames = cir.getReturnValue();
		if (originalNames.isEmpty() || NickClientConfig.getDefaultMode() == DisplayMode.HIDE) {
			return;
		}

		Map<String, String> nicknames = nick$collectNicknames();
		if (nicknames.isEmpty()) {
			return;
		}

		// Keep the real name too, so a nickname that shadows another name cannot make a
		// player unreachable from the suggestion list.
		Collection<String> result = new LinkedHashSet<>(originalNames.size() * 2);
		for (String name : originalNames) {
			result.add(nicknames.getOrDefault(name, name));
		}
		result.addAll(originalNames);
		cir.setReturnValue(new ArrayList<>(result));
	}

	private Map<String, String> nick$collectNicknames() {
		Map<String, String> result = new HashMap<>();
		MinecraftServer server = this.minecraft.getSingleplayerServer();
		if (server != null) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Component customName = player.getCustomName();
				if (customName != null) {
					result.put(player.getGameProfile().name(), customName.getString());
				}
			}
			return result;
		}
		if (this.minecraft.level != null) {
			for (Player player : this.minecraft.level.players()) {
				Component customName = player.getCustomName();
				if (customName != null) {
					result.put(player.getGameProfile().name(), customName.getString());
				}
			}
		}
		return result;
	}
}
