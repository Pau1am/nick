package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickClientConfig;
import com.dongchengqiao.nick.NickClientConfig.DisplayLocation;
import com.dongchengqiao.nick.NickClientConfig.DisplayMode;
import com.dongchengqiao.nick.NickClientNames;
import com.dongchengqiao.nick.NickDisplayText;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side tab list override, applying the per-location display mode.
 * <p>
 * The server already sends the nickname as the tab list name (see {@code NickTabListMixin}), so this
 * runs on the value vanilla produced instead of rebuilding it: {@code NICK_ONLY} then needs no work
 * at all, and the nickname is recovered from the tab list entry, which exists for every online
 * player - unlike the player entity, which is only present while the client tracks it.
 */
@Mixin(PlayerInfo.class)
public class NickClientTabListMixin {
	@Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
	private void nick$applyDisplayMode(CallbackInfoReturnable<Component> cir) {
		Component published = cir.getReturnValue();
		// A missing display name means the player has no nickname, so there is nothing to hide
		// or to combine, and vanilla's real name is already correct.
		if (published == null) {
			return;
		}

		DisplayMode mode = NickClientConfig.getDisplayMode(DisplayLocation.TAB_LIST);
		if (mode == DisplayMode.NICK_ONLY) {
			return;
		}

		PlayerInfo self = (PlayerInfo) (Object) this;
		String realName = self.getProfile().name();
		String nickname = NickClientNames.nicknameOf(self);
		cir.setReturnValue(NickClientNames.withTeam(
			NickClientNames.teamOf(realName),
			NickDisplayText.resolve(mode, realName, nickname)
		));
	}
}
