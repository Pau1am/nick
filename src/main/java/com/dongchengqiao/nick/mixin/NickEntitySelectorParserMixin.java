package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.UnicodeWordArgumentType;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets target selectors reference players by a non-ASCII nickname, e.g. {@code @p[name=小明]}.
 */
@Mixin(EntitySelectorParser.class)
public class NickEntitySelectorParserMixin {
	@Redirect(
		method = "parseNameOrUUID",
		at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readString()Ljava/lang/String;")
	)
	private String nick$readStringWithUnicode(StringReader reader) throws CommandSyntaxException {
		return UnicodeWordArgumentType.readWordOrQuoted(reader);
	}
}
