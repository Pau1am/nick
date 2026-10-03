package com.dongchengqiao.nick;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.*;

public final class NickCommand {
	/** Vanilla caps player names at 16 chars; give nicknames a bit more room but not unlimited. */
	private static final int MAX_NICK_LENGTH = 32;

	private static final SimpleCommandExceptionType ERROR_EMPTY =
		new SimpleCommandExceptionType(Component.translatableWithFallback(
			"nick.error.empty", "Nickname cannot be empty"));
	private static final SimpleCommandExceptionType ERROR_TOO_LONG =
		new SimpleCommandExceptionType(Component.translatableWithFallback(
			"nick.error.too_long", "Nickname is too long (max %s characters)", MAX_NICK_LENGTH));
	private static final SimpleCommandExceptionType ERROR_INVALID =
		new SimpleCommandExceptionType(Component.translatableWithFallback(
			"nick.error.invalid", "Nickname contains invalid characters"));
	private static final SimpleCommandExceptionType ERROR_TAKEN =
		new SimpleCommandExceptionType(Component.translatableWithFallback(
			"nick.error.taken", "That name is already used by another player"));

	private NickCommand() {
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("nick")
			.requires(source -> NickSettings.commandNick)
			.then(literal("set")
				.then(argument("name", UnicodeWordArgumentType.unicodeWord())
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						String nick = validate(ctx.getSource(), player, ctx.getArgument("name", String.class));
						applyNick(player, nick);
						ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(
							"nick.command.set", "Nickname set to: %s", nick), false);
						return 1;
					})
					.then(argument("target", EntityArgument.player())
						.requires(hasPermission(LEVEL_MODERATORS))
						.executes(ctx -> {
							ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
							String nick = validate(ctx.getSource(), target, ctx.getArgument("name", String.class));
							applyNick(target, nick);
							ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(
								"nick.command.set.other", "Set %s's nickname to: %s",
								target.getGameProfile().name(), nick), false);
							return 1;
						}))))
			.then(literal("reset")
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					resetNick(player);
					ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(
						"nick.command.reset", "Nickname reset"), false);
					return 1;
				})
				.then(argument("target", EntityArgument.player())
					.requires(hasPermission(LEVEL_MODERATORS))
					.executes(ctx -> {
						ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
						resetNick(target);
						ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(
							"nick.command.reset.other", "Reset %s's nickname",
							target.getGameProfile().name()), false);
						return 1;
					})))
		);
	}

	/**
	 * Rejects nicknames that would break command targeting or allow impersonation.
	 * Without this, a player could take another player's real name and make
	 * {@code /tp <name>} target the wrong person.
	 */
	private static String validate(CommandSourceStack source, ServerPlayer target, String rawNick)
		throws CommandSyntaxException {
		String nick = rawNick.strip();
		if (nick.isEmpty()) {
			throw ERROR_EMPTY.create();
		}
		if (nick.length() > MAX_NICK_LENGTH) {
			throw ERROR_TOO_LONG.create();
		}
		for (int i = 0; i < nick.length(); i++) {
			char c = nick.charAt(i);
			// '§' would inject colour/format codes, control chars would break chat and the tab list.
			if (c == '\u00a7' || c < 0x20 || c == 0x7f) {
				throw ERROR_INVALID.create();
			}
		}

		String targetName = target.getGameProfile().name();
		for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
			if (online != target && online.getGameProfile().name().equalsIgnoreCase(nick)) {
				throw ERROR_TAKEN.create();
			}
		}
		String owner = NickConfig.findOwner(nick);
		if (owner != null && !owner.equals(targetName)) {
			throw ERROR_TAKEN.create();
		}
		return nick;
	}

	private static void applyNick(ServerPlayer player, String nick) {
		NickConfig.setNick(player.getGameProfile().name(), nick);
		NickDisplay.apply(player, nick);
	}

	private static void resetNick(ServerPlayer player) {
		NickConfig.removeNick(player.getGameProfile().name());
		NickDisplay.reset(player);
	}
}
