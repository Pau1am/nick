package com.dongchengqiao.nick;

import net.fabricmc.api.ClientModInitializer;

/**
 * Client-only initialisation. The client config must not be touched on a dedicated
 * server, otherwise a useless {@code config/nick-client.json} shows up there.
 */
public class NickClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NickClientConfig.load();
	}
}
