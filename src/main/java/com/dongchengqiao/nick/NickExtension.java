package com.dongchengqiao.nick;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
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
				new RuleText("昵称命令", "启用或禁用 /nick 命令"),
				new RuleText("/player支持中文", "启用后 /player 命令可使用中文玩家名"),
				new RuleText("禁止非 ASCII 名称假人生成", "启用后 /player xxx spawn 拒绝含非 ASCII 字符的名称"));
			case "zh_tw" -> rules(
				new RuleText("暱稱指令", "啟用或停用 /nick 指令"),
				new RuleText("/player支援中文", "啟用後 /player 指令可使用中文玩家名"),
				new RuleText("禁止非 ASCII 名稱假人生成", "啟用後 /player xxx spawn 拒絕含非 ASCII 字元的名稱"));
			case "es_ar" -> rules(
				new RuleText("Comando Nick", "Activar o desactivar el comando /nick"),
				new RuleText("/player Soporte Chino", "Permite nombres chinos en /player"),
				new RuleText("Sin Fake Player no ASCII", "Evita /player xxx spawn con nombres que contengan caracteres no ASCII"));
			case "fr_fr" -> rules(
				new RuleText("Commande Nick", "Activer ou désactiver la commande /nick"),
				new RuleText("/player Support Chinois", "Permet les noms chinois dans /player"),
				new RuleText("Pas de PNJ non ASCII", "Empêche /player xxx spawn avec des noms contenant des caractères non ASCII"));
			case "pt_br" -> rules(
				new RuleText("Comando Nick", "Ativar ou desativar o comando /nick"),
				new RuleText("/player Suporte Chinês", "Permite nomes chineses em /player"),
				new RuleText("Sem Fake Player não ASCII", "Impede /player xxx spawn com nomes contendo caracteres não ASCII"));
			default -> rules(
				new RuleText("Nick Command", "Enable or disable the /nick command"),
				new RuleText("/player Chinese Support", "Allows Chinese player names in /player command"),
				new RuleText("No Non-ASCII Fake Player Spawn", "Prevents /player xxx spawn with names containing non-ASCII characters"));
		};
	}

	private record RuleText(String name, String description) {
	}

	/**
	 * Builds the Carpet rule translations for the three rules this extension adds. Takes the rules
	 * as named values rather than as six positional strings - with six, a caller could silently swap
	 * a name with a description, and {@code Map} would happily accept the result.
	 * <p>
	 * The description doubles as Carpet's {@code extra} text.
	 */
	private static Map<String, String> rules(RuleText nick, RuleText chineseSupport, RuleText noNonAsciiSpawn) {
		Map<String, String> translations = new LinkedHashMap<>();
		translations.put("carpet.category.NICK", "Nick");
		addRule(translations, "commandNick", nick);
		addRule(translations, "commandPlayerCN", chineseSupport);
		addRule(translations, "commandPlayerCNNoSpawn", noNonAsciiSpawn);
		return translations;
	}

	private static void addRule(Map<String, String> translations, String rule, RuleText text) {
		translations.put("carpet.rule." + rule + ".name", text.name());
		translations.put("carpet.rule." + rule + ".desc", text.description());
		translations.put("carpet.rule." + rule + ".extra", text.description());
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
