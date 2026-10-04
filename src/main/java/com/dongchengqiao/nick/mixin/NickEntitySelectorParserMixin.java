package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.UnicodeStrings;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets target selectors reference players by a non-ASCII nickname, e.g. {@code @p[name=小明]}.
 * This one is unconditional: it is what makes {@code name=} accept CJK even when the Carpet
 * rules that widen unquoted input globally are off.
 */
@Mixin(EntitySelectorParser.class)
public class NickEntitySelectorParserMixin {
	@Redirect(
		method = "parseNameOrUUID",
		at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readString()Ljava/lang/String;")
	)
	private String nick$readStringWithUnicode(StringReader reader) throws CommandSyntaxException {
		return UnicodeStrings.readWordOrQuoted(reader);
	}
}
