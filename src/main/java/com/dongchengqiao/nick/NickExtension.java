package com.dongchengqiao.nick;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class NickExtension implements CarpetExtension {
	/**
	 * Carpet calls {@code onGameStarted} again every time a world is loaded, so the observer must
	 * only be registered once - otherwise commands get re-sent N times per rule change.
	 */
	private static boolean observerRegistered = false;

	@Override
	public void onGameStarted() {
		CarpetServer.settingsManager.parseSettingsClass(NickSettings.class);
		if (observerRegistered) {
			return;
		}
		observerRegistered = true;
		SettingsManager.registerGlobalRuleObserver((source, rule, newValue) -> {
			// Only commandNick changes the command tree, so only it needs a refresh.
			if (!"commandNick".equals(rule.name()) || source == null || source.getServer() == null) {
				return;
			}
			for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
				source.getServer().getCommands().sendCommands(player);
			}
		});
	}

	@Override
	public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		NickCommand.registerCommands(dispatcher);
	}

	@Override
	public Map<String, String> canHasTranslations(String lang) {
		return switch (lang) {
			case "zh_cn" -> rules(
				"昵称命令", "启用或禁用 /nick 命令",
				"/player支持中文", "启用后 /player 命令可使用中文玩家名",
				"禁止非标准名称假人生成", "启用后 /player xxx spawn 拒绝含非字母、数字和下划线的名称");
			case "zh_tw" -> rules(
				"暱稱指令", "啟用或停用 /nick 指令",
				"/player支援中文", "啟用後 /player 指令可使用中文玩家名",
				"禁止非標準名稱假人生成", "啟用後 /player xxx spawn 拒絕含非字母、數字和底線的名稱");
			case "es_ar" -> rules(
				"Comando Nick", "Activar o desactivar el comando /nick",
				"/player Soporte Chino", "Permite nombres chinos en /player",
				"No Fake Player no Estándar", "Evita /player xxx spawn con nombres que contengan caracteres no alfanuméricos (excepto guión bajo)");
			case "fr_fr" -> rules(
				"Commande Nick", "Activer ou désactiver la commande /nick",
				"/player Support Chinois", "Permet les noms chinois dans /player",
				"Pas de PNJ non standard", "Empêche /player xxx spawn avec des noms contenant des caractères non alphanumériques (sauf trait de soulignement)");
			case "pt_br" -> rules(
				"Comando Nick", "Ativar ou desativar o comando /nick",
				"/player Suporte Chinês", "Permite nomes chineses em /player",
				"Sem Fake Player não Padrão", "Impede /player xxx spawn com nomes contendo caracteres não alfanuméricos (exceto sublinhado)");
			default -> rules(
				"Nick Command", "Enable or disable the /nick command",
				"/player Chinese Support", "Allows Chinese player names in /player command",
				"No Non-Standard Fake Player Spawn", "Prevents /player xxx spawn with names containing non-alphanumeric characters (except underscores)");
		};
	}

	/** Builds the Carpet rule translations; {@code desc} doubles as Carpet's {@code extra} text. */
	private static Map<String, String> rules(String nickName, String nickDesc,
											 String cnName, String cnDesc,
											 String noSpawnName, String noSpawnDesc) {
		return Map.ofEntries(
			Map.entry("carpet.category.NICK", "Nick"),
			Map.entry("carpet.rule.commandNick.name", nickName),
			Map.entry("carpet.rule.commandNick.desc", nickDesc),
			Map.entry("carpet.rule.commandNick.extra", nickDesc),
			Map.entry("carpet.rule.commandPlayerCN.name", cnName),
			Map.entry("carpet.rule.commandPlayerCN.desc", cnDesc),
			Map.entry("carpet.rule.commandPlayerCN.extra", cnDesc),
			Map.entry("carpet.rule.commandPlayerCNNoSpawn.name", noSpawnName),
			Map.entry("carpet.rule.commandPlayerCNNoSpawn.desc", noSpawnDesc),
			Map.entry("carpet.rule.commandPlayerCNNoSpawn.extra", noSpawnDesc)
		);
	}

	@Override
	public String version() {
		// Carpet prints this in its startup log; report the real mod version instead of the mod id.
		return FabricLoader.getInstance()
			.getModContainer("nick")
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}
}
