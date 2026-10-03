package ckpb1.client.mixin;

import ckpb1.modules.utility.ChatTranslator;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds every chat HUD message to the Chat Translator module. */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void ckpb1$onAddMessage(Text message, CallbackInfo ci) {
        ChatTranslator.onChatMessage(message);
    }
}
