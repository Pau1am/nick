package com.dongchengqiao.nick;

import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nick implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("nick");

	@Override
	public void onInitialize() {
		CarpetServer.manageExtension(new NickExtension());

		NickConfig.load();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;

			// Deferred by one server task on purpose: the JOIN event fires before the player's
			// saved data is applied, so reading or writing the display name here would be
			// overwritten by whatever was persisted in world/players/data. Waiting until after
			// that load is what makes the config the source of truth.
			server.execute(() -> {
				// A nickname can only be checked against players who are online at the time it is
				// set, so someone offline can still be impersonated. Once the real owner logs in we
				// can detect it, and the impersonating nickname has to go.
				releaseImpersonatedNames(player);

				syncNickname(player);
			});
		});
	}

	/**
	 * Clears any nickname that is now shadowing {@code joining}'s real name.
	 * Without this, {@code /tp <name>} would keep resolving to the impersonator.
	 */
	private static void releaseImpersonatedNames(ServerPlayer joining) {
		String realName = joining.getGameProfile().name();
		for (ServerPlayer online : joining.level().getServer().getPlayerList().getPlayers()) {
			if (online == joining) {
				continue;
			}
			Component customName = online.getCustomName();
			if (customName == null || !customName.getString().equalsIgnoreCase(realName)) {
				continue;
			}
			NickConfig.removeNick(online.getGameProfile().name());
			NickDisplay.reset(online);
			LOGGER.info("Cleared nickname '{}' from {} because {} has joined",
				customName.getString(), online.getGameProfile().name(), realName);
			online.sendSystemMessage(Component.translatableWithFallback(
				"nick.nickname_released",
				"Your nickname was reset because %s has joined the server", realName));
		}
	}

	/**
	 * Brings the player's display name in line with the config.
	 * <p>
	 * A stored nickname is re-validated instead of trusted: it may have been picked while its
	 * owner was offline, and the player it shadows could be online by now.
	 */
	private static void syncNickname(ServerPlayer player) {
		String realName = player.getGameProfile().name();
		String nick = NickConfig.getNick(realName);

		if (nick != null
			&& !NickNames.isMalformed(nick)
			&& !NickNames.isTaken(player.level().getServer(), player, nick)) {
			NickDisplay.apply(player, nick);
			return;
		}

		if (nick != null) {
			NickConfig.removeNick(realName);
			LOGGER.info("Dropped stored nickname '{}' for {} because it is no longer available", nick, realName);
			player.sendSystemMessage(Component.translatableWithFallback(
				"nick.nickname_dropped",
				"Your nickname '%s' was removed because that name is now in use", nick));
		}

		// The display name lives in the player data, so a nickname can outlive its config entry -
		// either because it was removed while the owner was offline, or because versions before
		// 1.1 wrote the entry under the nickname instead of the real name. The config is the
		// source of truth, so anything left over is cleared here.
		if (player.getCustomName() != null) {
			NickDisplay.reset(player);
		}
	}
}
