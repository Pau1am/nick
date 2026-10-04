package com.dongchengqiao.nick.mixin;

import com.dongchengqiao.nick.NickSettings;
import com.mojang.brigadier.StringReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets unquoted command input contain non-ASCII characters, so a bare Chinese nickname works
 * without quotes. Gated by the Carpet rules that need it (see {@code NickSettings#unicodeInputEnabled}).
 */
@Mixin(StringReader.class)
public class NickStringReaderMixin {
	@Inject(method = "isAllowedInUnquotedString", at = @At("RETURN"), cancellable = true)
	private static void nick$allowUnicodeInUnquotedString(char c, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && c > 127 && NickSettings.unicodeInputEnabled()) {
			cir.setReturnValue(true);
		}
	}
}
