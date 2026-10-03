package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickClientConfig;
import com.dongchengqiao.nick.NickClientConfig.DisplayLocation;
import com.dongchengqiao.nick.NickClientConfig.DisplayMode;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side name tag override. Vanilla renders {@code entity.getDisplayName()}, and for players
 * that resolves the real profile name, so the nickname only shows up here when the mod is present.
 */
@Mixin(EntityRenderer.class)
public class NickNameTagMixin {
	@Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
	private void nick$getNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
		if (!(entity instanceof Player player)) {
			return;
		}
		Component customName = player.getCustomName();
		if (customName == null) {
			return;
		}

		DisplayMode mode = NickClientConfig.getDisplayMode(DisplayLocation.NAMETAG);
		Component display = switch (mode) {
			case HIDE -> null; // vanilla already renders the real name
			case NICK_AND_ORIGINAL ->
				Component.literal("[" + customName.getString() + "]" + player.getGameProfile().name());
			default -> customName;
		};
		if (display == null) {
			return;
		}
		cir.setReturnValue(PlayerTeam.formatNameForTeam(player.getTeam(), display));
	}
}
