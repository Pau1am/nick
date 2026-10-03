package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickSettings;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rejects {@code /player <name> spawn} for non-ASCII names, which Carpet cannot create.
 */
@Mixin(targets = "carpet.commands.PlayerCommand")
public class NickCarpetPlayerCommandMixin {
	private static final SimpleCommandExceptionType SPAWN_CHINESE_ERROR =
		new SimpleCommandExceptionType(Component.translatableWithFallback(
			"nick.cant_spawn_chinese_name", "Cannot spawn fake player with Chinese name"));

	@Redirect(
		method = {"spawn", "cantSpawn"},
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/brigadier/arguments/StringArgumentType;getString(Lcom/mojang/brigadier/context/CommandContext;Ljava/lang/String;)Ljava/lang/String;"
		)
	)
	private static String nick$validateSpawnName(CommandContext<?> context, String argName) throws CommandSyntaxException {
		String name = context.getArgument(argName, String.class);
		if (NickSettings.commandPlayerCNNoSpawn && nick$hasNonAscii(name)) {
			throw SPAWN_CHINESE_ERROR.create();
		}
		return name;
	}

	private static boolean nick$hasNonAscii(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) > 127) {
				return true;
			}
		}
		return false;
	}
}
