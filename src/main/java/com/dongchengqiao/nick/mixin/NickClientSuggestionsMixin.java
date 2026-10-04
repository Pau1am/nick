package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickClientConfig;
import com.dongchengqiao.nick.NickClientConfig.DisplayLocation;
import com.dongchengqiao.nick.NickClientConfig.DisplayMode;
import com.dongchengqiao.nick.NickClientNames;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
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
 * see everywhere else. Honours the display mode configured for chat, since that is the context
 * these suggestions are typed in.
 */
@Mixin(ClientSuggestionProvider.class)
public class NickClientSuggestionsMixin {
	@Inject(method = "getOnlinePlayerNames", at = @At("RETURN"), cancellable = true)
	private void nick$getOnlinePlayerNames(CallbackInfoReturnable<Collection<String>> cir) {
		Collection<String> realNames = cir.getReturnValue();
		if (realNames.isEmpty() || NickClientConfig.getDisplayMode(DisplayLocation.CHAT) == DisplayMode.HIDE) {
			return;
		}

		Map<String, String> nicknames = nick$collectNicknames();
		if (nicknames.isEmpty()) {
			return;
		}

		// Keep the real name too, so a nickname that shadows another name cannot make a
		// player unreachable from the suggestion list.
		Collection<String> result = new LinkedHashSet<>(realNames.size() * 2);
		for (String name : realNames) {
			result.add(nicknames.getOrDefault(name, name));
		}
		result.addAll(realNames);
		cir.setReturnValue(new ArrayList<>(result));
	}

	private Map<String, String> nick$collectNicknames() {
		Map<String, String> result = new HashMap<>();
		for (PlayerInfo info : NickClientNames.onlinePlayers()) {
			String nickname = NickClientNames.nicknameOf(info);
			if (nickname != null) {
				result.put(info.getProfile().name(), nickname);
			}
		}
		return result;
	}
}
