package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The tab list entry the server sends to clients. Vanilla returns {@code null} here, which makes
 * the client fall back to the real profile name, so this is the only way to get a nickname into
 * the tab list for players who do not have the mod installed.
 */
@Mixin(ServerPlayer.class)
public abstract class NickTabListMixin {
	@Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
	private void nick$getTabListDisplayName(CallbackInfoReturnable<Component> cir) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		Component customName = self.getCustomName();
		if (customName == null) {
			return;
		}
		cir.setReturnValue(NickDisplay.decorate(self, customName));
	}
}
