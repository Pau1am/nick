package com.dongchengqiao.nick;

import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nick implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("nick");

	@Override
	public void onInitialize() {
		CarpetServer.manageExtension(new NickExtension());

		ArgumentTypeRegistry.registerArgumentType(
			Identifier.fromNamespaceAndPath("nick", "unicode_word"),
			UnicodeWordArgumentType.class,
			SingletonArgumentInfo.contextFree(UnicodeWordArgumentType::new)
		);

		NickConfig.load();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			// Always key the config by the real Mojang name, never by a display name.
			String nick = NickConfig.getNick(player.getGameProfile().name());
			if (nick != null) {
				NickDisplay.apply(player, nick);
			}
		});
	}
}
