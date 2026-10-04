package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickClientConfig;
import com.dongchengqiao.nick.NickClientConfig.DisplayLocation;
import com.dongchengqiao.nick.NickClientConfig.DisplayMode;
import com.dongchengqiao.nick.NickClientNames;
import com.dongchengqiao.nick.NickDisplayText;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side name tag override. Vanilla renders {@code entity.getDisplayName()}, and for players
 * that resolves the real profile name, so the nickname only shows up here when the mod is present.
 * <p>
 * Runs on vanilla's result rather than replacing it outright, so the hover player card vanilla puts
 * on a name tag survives on the nickname too.
 */
@Mixin(EntityRenderer.class)
public class NickNameTagMixin {
	@Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
	private void nick$applyDisplayMode(Entity entity, CallbackInfoReturnable<Component> cir) {
		if (!(entity instanceof Player player)) {
			return;
		}
		// A missing custom name means the player has no nickname, and vanilla's real name stands.
		Component customName = player.getCustomName();
		if (customName == null) {
			return;
		}

		DisplayMode mode = NickClientConfig.getDisplayMode(DisplayLocation.NAMETAG);
		// HIDE wants the real name, which is exactly what vanilla produced.
		if (mode == DisplayMode.HIDE) {
			return;
		}

		String realName = player.getGameProfile().name();
		Component display = NickDisplayText.resolve(mode, realName, customName.getString());
		Component vanilla = cir.getReturnValue();
		if (vanilla != null) {
			display = display.copy().withStyle(vanilla.getStyle());
		}
		cir.setReturnValue(NickClientNames.withTeam(player.getTeam(), display));
	}
}
